package handler

import (
	"net/http"
	"net/url"
	"sort"
	"strconv"
	"strings"
	"time"

	"github.com/go-chi/chi/v5"

	"github.com/mosinaRZ/finance-sync-backend/internal/domain/apperror"
	"github.com/mosinaRZ/finance-sync-backend/internal/domain/entity"
	"github.com/mosinaRZ/finance-sync-backend/internal/domain/repository"
	"github.com/mosinaRZ/finance-sync-backend/internal/interface/http/contextkeys"
	"github.com/mosinaRZ/finance-sync-backend/internal/interface/http/dto"
	"github.com/mosinaRZ/finance-sync-backend/internal/pkg/response"
	adminusecase "github.com/mosinaRZ/finance-sync-backend/internal/usecase/admin"
)

// AdminPanelHandler serves the admin console (user management, security events, analytics).
// Every route behind it is wrapped by middleware.RequireActiveAdmin.
type AdminPanelHandler struct {
	svc *adminusecase.PanelService
}

func NewAdminPanelHandler(svc *adminusecase.PanelService) *AdminPanelHandler {
	return &AdminPanelHandler{svc: svc}
}

func (h *AdminPanelHandler) actor(r *http.Request) adminusecase.Actor {
	userID, _ := contextkeys.UserID(r.Context())
	deviceID, _ := contextkeys.DeviceID(r.Context())
	ip, _ := contextkeys.ClientIP(r.Context())
	return adminusecase.Actor{UserID: userID, DeviceID: deviceID, IP: ip}
}

func ms(t time.Time) int64 {
	if t.IsZero() {
		return 0
	}
	return t.UnixMilli()
}

func intParam(q url.Values, key string, def, min, max int) int {
	v, err := strconv.Atoi(strings.TrimSpace(q.Get(key)))
	if err != nil {
		return def
	}
	if v < min {
		return min
	}
	if v > max {
		return max
	}
	return v
}

func oneOf(value string, allowed ...string) (string, error) {
	if value == "" {
		return "", nil
	}
	for _, a := range allowed {
		if value == a {
			return value, nil
		}
	}
	return "", apperror.ErrValidation("invalid filter value")
}

func parseTypes(raw string) ([]string, error) {
	if strings.TrimSpace(raw) == "" {
		return nil, nil
	}
	parts := strings.Split(raw, ",")
	out := make([]string, 0, len(parts))
	for _, p := range parts {
		p = strings.TrimSpace(p)
		if p == "" {
			continue
		}
		if !entity.ValidSecurityEventType(p) {
			return nil, apperror.ErrValidation("invalid event type")
		}
		out = append(out, p)
	}
	return out, nil
}

func userResponse(row *repository.AdminUserRow, window time.Duration) dto.AdminUserResponse {
	u := row.User
	seen := map[string]struct{}{}
	platforms := []string{}
	appVersion := ""
	var newest time.Time
	for _, d := range row.Devices {
		if d.Platform != "" {
			if _, ok := seen[d.Platform]; !ok {
				seen[d.Platform] = struct{}{}
				platforms = append(platforms, d.Platform)
			}
		}
		if d.AppVersion != "" && !d.LastSeenAt.Before(newest) {
			newest, appVersion = d.LastSeenAt, d.AppVersion
		}
	}
	sort.Strings(platforms)
	resp := dto.AdminUserResponse{
		ID: u.ID, PhoneNumber: u.PhoneNumber, PhoneVerified: u.PhoneVerified, FirstName: u.FirstName, LastName: u.LastName,
		Gender: string(u.Gender), BirthDate: u.BirthDate, Role: string(u.Role), CreatedAtMs: ms(u.CreatedAt), UpdatedAtMs: ms(u.UpdatedAt),
		IsDeleted: u.IsDeleted, FailedLoginAttempts: u.FailedLoginAttempts, Online: row.Online, LastSeenAtMs: ms(row.LastSeenAt),
		DeviceCount: len(row.Devices), Platforms: platforms, AppVersion: appVersion, SyncRecords: row.SyncRecords, SyncBytes: row.SyncBytes,
	}
	if u.DeletedAt != nil {
		resp.DeletedAtMs = ms(*u.DeletedAt)
	}
	if u.LockedUntil != nil && u.LockedUntil.After(time.Now()) {
		resp.LockedUntilMs = ms(*u.LockedUntil)
	}
	return resp
}

func eventResponse(e adminusecase.LabelledEvent) dto.AdminEventResponse {
	ev := e.Event
	return dto.AdminEventResponse{
		ID: ev.ID, Type: string(ev.Type), Severity: entity.SeverityOf(ev.Type), UserID: ev.UserID, UserLabel: e.UserLabel,
		ActorUserID: ev.ActorUserID, ActorLabel: e.ActorLabel, DeviceName: ev.DeviceName, Platform: ev.Platform,
		AppVersion: ev.AppVersion, IP: maskIP(ev.IP), Detail: ev.Detail, CreatedAtMs: ms(ev.CreatedAt),
	}
}

// Me confirms that the caller is an active administrator (the middleware already enforced it).
func (h *AdminPanelHandler) Me(w http.ResponseWriter, r *http.Request) {
	response.JSON(w, http.StatusOK, dto.AdminMeResponse{UserID: h.actor(r).UserID, Role: string(entity.RoleAdmin)})
}

func (h *AdminPanelHandler) ListUsers(w http.ResponseWriter, r *http.Request) {
	q := r.URL.Query()
	role, err := oneOf(q.Get("role"), "user", "pro", "premium", "admin")
	if err != nil {
		response.Error(w, err)
		return
	}
	status, err := oneOf(q.Get("status"), "active", "deleted", "locked")
	if err != nil {
		response.Error(w, err)
		return
	}
	presence, err := oneOf(q.Get("presence"), "online", "offline")
	if err != nil {
		response.Error(w, err)
		return
	}
	sortBy, err := oneOf(q.Get("sort"), "newest", "oldest", "name")
	if err != nil {
		response.Error(w, err)
		return
	}
	limit := intParam(q, "limit", 30, 1, 100)
	offset := intParam(q, "offset", 0, 0, 1000000)
	rows, total, err := h.svc.ListUsers(r.Context(), repository.AdminUserFilter{
		Search: q.Get("q"), Role: role, Status: status, Presence: presence, Sort: sortBy, Limit: limit, Offset: offset,
	})
	if err != nil {
		response.Error(w, err)
		return
	}
	window := h.svc.OnlineWindow()
	users := make([]dto.AdminUserResponse, 0, len(rows))
	for i := range rows {
		users = append(users, userResponse(&rows[i], window))
	}
	response.JSON(w, http.StatusOK, dto.AdminUserListResponse{Users: users, Total: total, Limit: limit, Offset: offset, OnlineWindowSec: int64(window / time.Second)})
}

func (h *AdminPanelHandler) GetUser(w http.ResponseWriter, r *http.Request) {
	detail, err := h.svc.GetUser(r.Context(), chi.URLParam(r, "userID"))
	if err != nil {
		response.Error(w, err)
		return
	}
	window := h.svc.OnlineWindow()
	cutoff := time.Now().Add(-window)
	devices := make([]dto.AdminDeviceResponse, 0, len(detail.Row.Devices))
	for _, d := range detail.Row.Devices {
		devices = append(devices, dto.AdminDeviceResponse{
			ID: d.ID, Name: d.Name, Model: d.Model, Platform: d.Platform, OSVersion: d.OSVersion, AppVersion: d.AppVersion,
			LastIP: maskIP(d.LastIP), LastSeenAtMs: ms(d.LastSeenAt), CreatedAtMs: ms(d.CreatedAt), Online: d.LastSeenAt.After(cutoff),
		})
	}
	events := make([]dto.AdminEventResponse, 0, len(detail.Events))
	for _, e := range detail.Events {
		events = append(events, eventResponse(adminusecase.LabelledEvent{Event: e}))
	}
	response.JSON(w, http.StatusOK, dto.AdminUserDetailResponse{User: userResponse(detail.Row, window), Devices: devices, Events: events})
}

func (h *AdminPanelHandler) UpdateUser(w http.ResponseWriter, r *http.Request) {
	var q dto.UpdateAdminUserRequest
	if err := decodeJSON(w, r, &q); err != nil {
		response.Error(w, err)
		return
	}
	in := adminusecase.UpdateUserInput{FirstName: q.FirstName, LastName: q.LastName}
	if q.Role != nil {
		role := entity.Role(strings.TrimSpace(*q.Role))
		if !role.Valid() {
			response.Error(w, apperror.ErrValidation("invalid user role"))
			return
		}
		in.Role = &role
	}
	row, err := h.svc.UpdateUser(r.Context(), h.actor(r), chi.URLParam(r, "userID"), in)
	if err != nil {
		response.Error(w, err)
		return
	}
	response.JSON(w, http.StatusOK, userResponse(row, h.svc.OnlineWindow()))
}

func (h *AdminPanelHandler) respondRow(w http.ResponseWriter, row *repository.AdminUserRow, err error) {
	if err != nil {
		response.Error(w, err)
		return
	}
	response.JSON(w, http.StatusOK, userResponse(row, h.svc.OnlineWindow()))
}

func (h *AdminPanelHandler) DeleteUser(w http.ResponseWriter, r *http.Request) {
	row, err := h.svc.DeleteUser(r.Context(), h.actor(r), chi.URLParam(r, "userID"))
	h.respondRow(w, row, err)
}

func (h *AdminPanelHandler) RestoreUser(w http.ResponseWriter, r *http.Request) {
	row, err := h.svc.RestoreUser(r.Context(), h.actor(r), chi.URLParam(r, "userID"))
	h.respondRow(w, row, err)
}

func (h *AdminPanelHandler) UnlockUser(w http.ResponseWriter, r *http.Request) {
	row, err := h.svc.UnlockUser(r.Context(), h.actor(r), chi.URLParam(r, "userID"))
	h.respondRow(w, row, err)
}

func (h *AdminPanelHandler) RevokeSessions(w http.ResponseWriter, r *http.Request) {
	row, err := h.svc.RevokeSessions(r.Context(), h.actor(r), chi.URLParam(r, "userID"))
	h.respondRow(w, row, err)
}

func (h *AdminPanelHandler) Events(w http.ResponseWriter, r *http.Request) {
	q := r.URL.Query()
	severity, err := oneOf(q.Get("severity"), entity.SeverityInfo, entity.SeverityWarning, entity.SeverityDanger)
	if err != nil {
		response.Error(w, err)
		return
	}
	types, err := parseTypes(q.Get("type"))
	if err != nil {
		response.Error(w, err)
		return
	}
	days := intParam(q, "days", 30, 1, 365)
	limit := intParam(q, "limit", 50, 1, 200)
	offset := intParam(q, "offset", 0, 0, 1000000)
	list, total, err := h.svc.Events(r.Context(), repository.SecurityEventFilter{
		Types: types, Severity: severity, UserID: strings.TrimSpace(q.Get("user_id")),
		From: time.Now().UTC().AddDate(0, 0, -days), Limit: limit, Offset: offset,
	})
	if err != nil {
		response.Error(w, err)
		return
	}
	out := make([]dto.AdminEventResponse, 0, len(list))
	for _, e := range list {
		out = append(out, eventResponse(e))
	}
	response.JSON(w, http.StatusOK, dto.AdminEventListResponse{Events: out, Total: total, Limit: limit, Offset: offset})
}

func bucketsDTO(in []repository.Bucket) []dto.AdminBucket {
	out := make([]dto.AdminBucket, 0, len(in))
	for _, b := range in {
		out = append(out, dto.AdminBucket{Key: b.Key, Count: b.Count})
	}
	return out
}

func (h *AdminPanelHandler) Analytics(w http.ResponseWriter, r *http.Request) {
	q := r.URL.Query()
	severity, err := oneOf(q.Get("severity"), entity.SeverityInfo, entity.SeverityWarning, entity.SeverityDanger)
	if err != nil {
		response.Error(w, err)
		return
	}
	platform, err := oneOf(q.Get("platform"), "android", "ios", "web", "desktop")
	if err != nil {
		response.Error(w, err)
		return
	}
	types, err := parseTypes(q.Get("type"))
	if err != nil {
		response.Error(w, err)
		return
	}
	a, err := h.svc.Analytics(r.Context(), adminusecase.AnalyticsQuery{
		Days: intParam(q, "days", 30, 1, 365), TzOffsetMin: intParam(q, "tz", 0, -720, 840),
		UserID: strings.TrimSpace(q.Get("user_id")), Severity: severity, Types: types, Platform: platform,
	})
	if err != nil {
		response.Error(w, err)
		return
	}
	o := a.Overview
	resp := dto.AdminAnalyticsResponse{
		FromMs: ms(a.From), ToMs: ms(a.To), Days: a.Days,
		KPIs: dto.AdminKPIs{
			TotalUsers: o.TotalUsers, ActiveUsers: o.ActiveUsers, DeletedUsers: o.DeletedUsers, LockedUsers: o.LockedUsers,
			OnlineNow: o.OnlineNow, NewUsers: o.NewInPeriod, LoginsSuccess: a.LoginsSuccess, LoginsFailed: a.LoginsFailed,
			UniqueLoginUsers: a.UniqueLoginUsers, DangerEvents: a.DangerEvents, LockedEvents: a.LockedEvents,
			SyncRecords: o.SyncRecords, SyncBytes: o.SyncBytes,
		},
		Risk:        dto.AdminRisk{Score: a.Risk.Score, Level: a.Risk.Level},
		EventTypes:  bucketsDTO(a.EventTypes),
		Roles:       bucketsDTO(o.Roles),
		Platforms:   bucketsDTO(o.Platforms),
		AppVersions: bucketsDTO(o.AppVersions), SyncByEntity: bucketsDTO(o.SyncByEntity),
		EventsTruncated: a.EventsTruncated, EventsConsidered: a.EventsConsidered, OnlineWindowSec: a.OnlineWindowSecond,
		LoginsByDay: []dto.AdminLoginDay{}, SeverityByDay: []dto.AdminSeverityDay{}, SignupsByDay: []dto.AdminSignupDay{},
		SyncByDay: []dto.AdminSyncDay{}, TopSyncUsers: []dto.AdminTopSyncUser{}, Suspicious: []dto.AdminSuspiciousSource{},
	}
	for _, d := range a.LoginsByDay {
		resp.LoginsByDay = append(resp.LoginsByDay, dto.AdminLoginDay{Day: d.Day, Success: d.Success, Failed: d.Failed})
	}
	for _, d := range a.SeverityByDay {
		resp.SeverityByDay = append(resp.SeverityByDay, dto.AdminSeverityDay{Day: d.Day, Info: d.Info, Warning: d.Warning, Danger: d.Danger})
	}
	// Fill the gaps so every chart gets one point per day of the range.
	signups := map[string]int64{}
	for _, d := range o.SignupsByDay {
		signups[d.Day] = d.Records
	}
	syncDays := map[string]repository.DayPoint{}
	for _, d := range o.SyncByDay {
		syncDays[d.Day] = d
	}
	for _, d := range a.LoginsByDay {
		resp.SignupsByDay = append(resp.SignupsByDay, dto.AdminSignupDay{Day: d.Day, Count: signups[d.Day]})
		p := syncDays[d.Day]
		resp.SyncByDay = append(resp.SyncByDay, dto.AdminSyncDay{Day: d.Day, Records: p.Records, Bytes: p.Bytes})
	}
	resp.Heatmap = make([][]int64, 7)
	for i := 0; i < 7; i++ {
		resp.Heatmap[i] = append([]int64(nil), a.Heatmap[i][:]...)
	}
	for _, t := range o.TopSyncUsers {
		resp.TopSyncUsers = append(resp.TopSyncUsers, dto.AdminTopSyncUser{UserID: t.UserID, Label: a.TopSyncLabels[t.UserID], Records: t.Records, Bytes: t.Bytes})
	}
	for _, s := range a.Suspicious {
		resp.Suspicious = append(resp.Suspicious, dto.AdminSuspiciousSource{IP: maskIP(s.IP), Failed: s.Failed, Accounts: s.Accounts})
	}
	response.JSON(w, http.StatusOK, resp)
}
