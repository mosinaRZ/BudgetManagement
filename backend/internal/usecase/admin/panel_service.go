package admin

import (
	"context"
	"sort"
	"strings"
	"time"
	"unicode"

	"github.com/mosinaRZ/finance-sync-backend/internal/domain/apperror"
	"github.com/mosinaRZ/finance-sync-backend/internal/domain/entity"
	"github.com/mosinaRZ/finance-sync-backend/internal/domain/repository"
)

// DefaultOnlineWindow is how recently a device must have been seen for its owner to count as
// online. Clients poll the backend about once a minute while the app is open and presence is
// refreshed at most every 45 seconds, so three minutes tolerates a missed poll.
const DefaultOnlineWindow = 3 * time.Minute

const (
	maxAnalyticsDays   = 365
	maxAnalyticsEvents = 50000
	maxNameRunes       = 80
)

// Actor identifies the administrator performing an action (for the audit trail).
type Actor struct {
	UserID   string
	DeviceID string
	IP       string
}

// PanelService backs the admin console: user management, security events and analytics.
type PanelService struct {
	users        repository.UserRepository
	query        repository.AdminQueryRepository
	events       repository.SecurityEventRepository
	refresh      repository.RefreshTokenRepository
	audit        repository.AuditLogRepository
	onlineWindow time.Duration
}

func NewPanelService(
	users repository.UserRepository,
	query repository.AdminQueryRepository,
	events repository.SecurityEventRepository,
	refresh repository.RefreshTokenRepository,
	audit repository.AuditLogRepository,
) *PanelService {
	return &PanelService{users: users, query: query, events: events, refresh: refresh, audit: audit, onlineWindow: DefaultOnlineWindow}
}

// OnlineWindow exposes the presence window so clients can describe it.
func (s *PanelService) OnlineWindow() time.Duration { return s.onlineWindow }

// IsActiveAdmin re-reads the account: a role claim inside a still-valid access token must never
// be trusted on its own for destructive endpoints (the admin may have been demoted or deleted
// since the token was issued).
func (s *PanelService) IsActiveAdmin(ctx context.Context, userID string) (bool, error) {
	u, err := s.users.FindByID(ctx, userID)
	if err != nil {
		if code, ok := apperror.CodeOf(err); ok && code == apperror.CodeNotFound {
			return false, nil
		}
		return false, err
	}
	return u != nil && !u.IsDeleted && u.Role == entity.RoleAdmin, nil
}

// RecordDenied logs an attempt by a non-admin to reach an admin endpoint.
func (s *PanelService) RecordDenied(ctx context.Context, a Actor, path string) {
	s.record(ctx, entity.SecAdminDenied, "", a, path)
}

func (s *PanelService) record(ctx context.Context, t entity.SecurityEventType, target string, a Actor, detail string) {
	if s.events == nil {
		return
	}
	user := target
	actor := a.UserID
	if t == entity.SecAdminDenied {
		user, actor = a.UserID, ""
	}
	rec, cancel := context.WithTimeout(context.WithoutCancel(ctx), 3*time.Second)
	defer cancel()
	_ = s.events.Record(rec, &entity.SecurityEvent{
		Type: t, UserID: user, ActorUserID: actor, DeviceID: a.DeviceID, IP: a.IP, Detail: detail, CreatedAt: time.Now().UTC(),
	})
}

// ---------------------------------------------------------------- users

func (s *PanelService) ListUsers(ctx context.Context, f repository.AdminUserFilter) ([]repository.AdminUserRow, int64, error) {
	f.OnlineWindow = s.onlineWindow
	return s.query.ListUsers(ctx, f)
}

// UserDetail is one account plus its most recent security events.
type UserDetail struct {
	Row    *repository.AdminUserRow
	Events []*entity.SecurityEvent
}

func (s *PanelService) GetUser(ctx context.Context, id string) (*UserDetail, error) {
	row, err := s.query.GetUser(ctx, id, s.onlineWindow)
	if err != nil {
		return nil, err
	}
	detail := &UserDetail{Row: row}
	if s.events != nil {
		list, _, err := s.events.List(ctx, repository.SecurityEventFilter{UserID: id, Limit: 20})
		if err != nil {
			return nil, err
		}
		detail.Events = list
	}
	return detail, nil
}

type UpdateUserInput struct {
	FirstName *string
	LastName  *string
	Role      *entity.Role
}

func cleanName(v string) string {
	var b strings.Builder
	for _, r := range strings.ToValidUTF8(v, "") {
		switch {
		case r == '\u200c':
			b.WriteRune(r)
		case unicode.IsSpace(r):
			b.WriteRune(' ')
		case unicode.IsControl(r), unicode.Is(unicode.Cf, r), unicode.Is(unicode.Co, r):
		default:
			b.WriteRune(r)
		}
	}
	return strings.Join(strings.Fields(b.String()), " ")
}

func (s *PanelService) UpdateUser(ctx context.Context, a Actor, targetID string, in UpdateUserInput) (*repository.AdminUserRow, error) {
	row, err := s.query.GetUser(ctx, targetID, s.onlineWindow)
	if err != nil {
		return nil, err
	}
	u := row.User
	if u.IsDeleted {
		return nil, apperror.ErrConflict("a deleted account cannot be edited; restore it first")
	}
	first, last := u.FirstName, u.LastName
	profileChanged := []string{}
	if in.FirstName != nil {
		v := cleanName(*in.FirstName)
		if len([]rune(v)) > maxNameRunes {
			return nil, apperror.ErrValidation("first name is too long")
		}
		if v != first {
			first = v
			profileChanged = append(profileChanged, "first_name")
		}
	}
	if in.LastName != nil {
		v := cleanName(*in.LastName)
		if len([]rune(v)) > maxNameRunes {
			return nil, apperror.ErrValidation("last name is too long")
		}
		if v != last {
			last = v
			profileChanged = append(profileChanged, "last_name")
		}
	}
	var newRole entity.Role
	roleChanged := false
	if in.Role != nil && *in.Role != u.Role {
		newRole = *in.Role
		if !newRole.Valid() {
			return nil, apperror.ErrValidation("invalid user role")
		}
		if a.UserID == targetID {
			return nil, apperror.ErrForbidden("you cannot change your own role")
		}
		if u.Role == entity.RoleAdmin {
			if err := s.ensureAnotherAdmin(ctx); err != nil {
				return nil, err
			}
		}
		roleChanged = true
	}
	if len(profileChanged) > 0 {
		if err := s.users.UpdateProfile(ctx, targetID, first, last, u.Gender, u.BirthDate); err != nil {
			return nil, err
		}
		s.record(ctx, entity.SecAdminUserEdited, targetID, a, strings.Join(profileChanged, ","))
	}
	if roleChanged {
		if err := s.users.UpdateRole(ctx, targetID, newRole); err != nil {
			return nil, err
		}
		s.record(ctx, entity.SecAdminRoleChanged, targetID, a, string(u.Role)+">"+string(newRole))
		if s.audit != nil {
			_ = s.audit.Create(ctx, &entity.AuditLog{
				ActorUserID: a.UserID, TargetUserID: targetID, Action: "user_role_updated",
				OldValue: string(u.Role), NewValue: string(newRole), CreatedAt: time.Now().UTC(),
			})
		}
	}
	return s.query.GetUser(ctx, targetID, s.onlineWindow)
}

// ensureAnotherAdmin refuses to remove the last active administrator, which would lock everyone
// out of the console.
func (s *PanelService) ensureAnotherAdmin(ctx context.Context) error {
	n, err := s.query.CountActiveAdmins(ctx)
	if err != nil {
		return err
	}
	if n <= 1 {
		return apperror.ErrConflict("at least one active administrator must remain")
	}
	return nil
}

func (s *PanelService) DeleteUser(ctx context.Context, a Actor, targetID string) (*repository.AdminUserRow, error) {
	if a.UserID == targetID {
		return nil, apperror.ErrForbidden("you cannot delete your own account from the admin console")
	}
	row, err := s.query.GetUser(ctx, targetID, s.onlineWindow)
	if err != nil {
		return nil, err
	}
	if row.User.IsDeleted {
		return nil, apperror.ErrConflict("account is already deleted")
	}
	if row.User.Role == entity.RoleAdmin {
		if err := s.ensureAnotherAdmin(ctx); err != nil {
			return nil, err
		}
	}
	if _, err := s.users.SetDeleted(ctx, targetID, true, time.Now().UTC()); err != nil {
		return nil, err
	}
	// Same soft-delete the owner would trigger: data stays, access ends immediately.
	_ = s.refresh.RevokeAllForUser(ctx, targetID)
	s.record(ctx, entity.SecAdminUserDeleted, targetID, a, "soft_delete")
	return s.query.GetUser(ctx, targetID, s.onlineWindow)
}

func (s *PanelService) RestoreUser(ctx context.Context, a Actor, targetID string) (*repository.AdminUserRow, error) {
	row, err := s.query.GetUser(ctx, targetID, s.onlineWindow)
	if err != nil {
		return nil, err
	}
	if !row.User.IsDeleted {
		return nil, apperror.ErrConflict("account is not deleted")
	}
	if _, err := s.users.SetDeleted(ctx, targetID, false, time.Now().UTC()); err != nil {
		return nil, err
	}
	s.record(ctx, entity.SecAdminUserRestored, targetID, a, "restore")
	return s.query.GetUser(ctx, targetID, s.onlineWindow)
}

func (s *PanelService) UnlockUser(ctx context.Context, a Actor, targetID string) (*repository.AdminUserRow, error) {
	if _, err := s.query.GetUser(ctx, targetID, s.onlineWindow); err != nil {
		return nil, err
	}
	if err := s.users.ResetFailedLogin(ctx, targetID); err != nil {
		return nil, err
	}
	s.record(ctx, entity.SecAdminUnlocked, targetID, a, "")
	return s.query.GetUser(ctx, targetID, s.onlineWindow)
}

func (s *PanelService) RevokeSessions(ctx context.Context, a Actor, targetID string) (*repository.AdminUserRow, error) {
	if a.UserID == targetID {
		return nil, apperror.ErrForbidden("you cannot revoke your own sessions from the admin console")
	}
	if _, err := s.query.GetUser(ctx, targetID, s.onlineWindow); err != nil {
		return nil, err
	}
	if _, err := s.users.IncrementSessionVersion(ctx, targetID); err != nil {
		return nil, err
	}
	if err := s.refresh.RevokeAllForUser(ctx, targetID); err != nil {
		return nil, err
	}
	s.record(ctx, entity.SecAdminRevoked, targetID, a, "")
	return s.query.GetUser(ctx, targetID, s.onlineWindow)
}

// ---------------------------------------------------------------- events

// LabelledEvent is an event with human-readable names for the accounts involved.
type LabelledEvent struct {
	Event      *entity.SecurityEvent
	UserLabel  string
	ActorLabel string
}

func (s *PanelService) label(ctx context.Context, events []*entity.SecurityEvent) []LabelledEvent {
	ids := map[string]struct{}{}
	for _, e := range events {
		if e.UserID != "" {
			ids[e.UserID] = struct{}{}
		}
		if e.ActorUserID != "" {
			ids[e.ActorUserID] = struct{}{}
		}
	}
	list := make([]string, 0, len(ids))
	for id := range ids {
		list = append(list, id)
	}
	labels, err := s.query.Labels(ctx, list)
	if err != nil {
		labels = map[string]string{}
	}
	out := make([]LabelledEvent, 0, len(events))
	for _, e := range events {
		out = append(out, LabelledEvent{Event: e, UserLabel: labels[e.UserID], ActorLabel: labels[e.ActorUserID]})
	}
	return out
}

func (s *PanelService) Events(ctx context.Context, f repository.SecurityEventFilter) ([]LabelledEvent, int64, error) {
	if s.events == nil {
		return nil, 0, nil
	}
	list, total, err := s.events.List(ctx, f)
	if err != nil {
		return nil, 0, err
	}
	return s.label(ctx, list), total, nil
}

// ---------------------------------------------------------------- analytics

type AnalyticsQuery struct {
	Days        int
	TzOffsetMin int
	UserID      string
	Severity    string
	Types       []string
	Platform    string
}

type LoginDay struct {
	Day     string
	Success int64
	Failed  int64
}

type SeverityDay struct {
	Day                   string
	Info, Warning, Danger int64
}

type SuspiciousSource struct {
	IP       string
	Failed   int64
	Accounts int64
}

type Risk struct {
	Score int
	Level string
}

type Analytics struct {
	From, To           time.Time
	Days               int
	Overview           repository.AdminOverview
	LoginsByDay        []LoginDay
	SeverityByDay      []SeverityDay
	Heatmap            [7][24]int64
	EventTypes         []repository.Bucket
	LoginsSuccess      int64
	LoginsFailed       int64
	UniqueLoginUsers   int64
	DangerEvents       int64
	LockedEvents       int64
	Suspicious         []SuspiciousSource
	TopSyncLabels      map[string]string
	Risk               Risk
	EventsTruncated    bool
	EventsConsidered   int
	OnlineWindowSecond int64
}

func (s *PanelService) Analytics(ctx context.Context, q AnalyticsQuery) (*Analytics, error) {
	days := q.Days
	if days <= 0 {
		days = 30
	}
	if days > maxAnalyticsDays {
		days = maxAnalyticsDays
	}
	off := q.TzOffsetMin
	if off < -720 {
		off = -720
	}
	if off > 840 {
		off = 840
	}
	loc := time.FixedZone("client", off*60)
	nowLocal := time.Now().In(loc)
	startLocal := time.Date(nowLocal.Year(), nowLocal.Month(), nowLocal.Day(), 0, 0, 0, 0, loc).AddDate(0, 0, -(days - 1))
	from := startLocal.UTC()
	to := startLocal.AddDate(0, 0, days).UTC()

	overview, err := s.query.Overview(ctx, from, to, off, s.onlineWindow, 200000)
	if err != nil {
		return nil, err
	}
	out := &Analytics{From: from, To: to, Days: days, Overview: overview, OnlineWindowSecond: int64(s.onlineWindow / time.Second)}

	dayIndex := map[string]int{}
	for i := 0; i < days; i++ {
		d := startLocal.AddDate(0, 0, i).Format("2006-01-02")
		dayIndex[d] = i
		out.LoginsByDay = append(out.LoginsByDay, LoginDay{Day: d})
		out.SeverityByDay = append(out.SeverityByDay, SeverityDay{Day: d})
	}

	if s.events != nil {
		events, err := s.events.Between(ctx, from, to, repository.SecurityEventFilter{
			UserID: q.UserID, Severity: q.Severity, Types: q.Types,
		}, maxAnalyticsEvents)
		if err != nil {
			return nil, err
		}
		out.EventsTruncated = len(events) >= maxAnalyticsEvents
		type ipStat struct {
			failed   int64
			accounts map[string]struct{}
		}
		ips := map[string]*ipStat{}
		uniq := map[string]struct{}{}
		types := map[string]int64{}
		for _, e := range events {
			if q.Platform != "" && e.Platform != q.Platform {
				continue
			}
			out.EventsConsidered++
			local := e.CreatedAt.In(loc)
			idx, ok := dayIndex[local.Format("2006-01-02")]
			types[string(e.Type)]++
			sev := entity.SeverityOf(e.Type)
			if ok {
				switch sev {
				case entity.SeverityDanger:
					out.SeverityByDay[idx].Danger++
				case entity.SeverityWarning:
					out.SeverityByDay[idx].Warning++
				default:
					out.SeverityByDay[idx].Info++
				}
			}
			if sev == entity.SeverityDanger {
				out.DangerEvents++
			}
			switch e.Type {
			case entity.SecLoginSuccess:
				out.LoginsSuccess++
				if ok {
					out.LoginsByDay[idx].Success++
				}
				out.Heatmap[int(local.Weekday())][local.Hour()]++
				if e.UserID != "" {
					uniq[e.UserID] = struct{}{}
				}
			case entity.SecLoginFailed:
				out.LoginsFailed++
				if ok {
					out.LoginsByDay[idx].Failed++
				}
				if e.IP != "" {
					st := ips[e.IP]
					if st == nil {
						st = &ipStat{accounts: map[string]struct{}{}}
						ips[e.IP] = st
					}
					st.failed++
					if e.UserID != "" {
						st.accounts[e.UserID] = struct{}{}
					}
				}
			case entity.SecAccountLocked:
				out.LockedEvents++
			}
		}
		out.UniqueLoginUsers = int64(len(uniq))
		for k, v := range types {
			out.EventTypes = append(out.EventTypes, repository.Bucket{Key: k, Count: v})
		}
		sort.Slice(out.EventTypes, func(i, j int) bool { return out.EventTypes[i].Count > out.EventTypes[j].Count })
		for ip, st := range ips {
			out.Suspicious = append(out.Suspicious, SuspiciousSource{IP: ip, Failed: st.failed, Accounts: int64(len(st.accounts))})
		}
		sort.Slice(out.Suspicious, func(i, j int) bool { return out.Suspicious[i].Failed > out.Suspicious[j].Failed })
		if len(out.Suspicious) > 5 {
			out.Suspicious = out.Suspicious[:5]
		}
	}

	ids := make([]string, 0, len(overview.TopSyncUsers))
	for _, t := range overview.TopSyncUsers {
		ids = append(ids, t.UserID)
	}
	if labels, err := s.query.Labels(ctx, ids); err == nil {
		out.TopSyncLabels = labels
	} else {
		out.TopSyncLabels = map[string]string{}
	}
	out.Risk = computeRisk(out)
	return out, nil
}

// computeRisk turns the period's security signals into one 0-100 number for the gauge. It is a
// deliberately simple, explainable heuristic (not a verdict): dangerous events weigh most, then
// accounts that actually hit the lockout, then the share of failed sign-ins.
func computeRisk(a *Analytics) Risk {
	score := a.DangerEvents*3 + a.LockedEvents*8
	if total := a.LoginsSuccess + a.LoginsFailed; total > 0 {
		score += a.LoginsFailed * 40 / total
	}
	if score > 100 {
		score = 100
	}
	level := "low"
	switch {
	case score >= 50:
		level = "high"
	case score >= 20:
		level = "medium"
	}
	return Risk{Score: int(score), Level: level}
}
