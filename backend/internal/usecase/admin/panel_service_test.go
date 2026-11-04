package admin

import (
	"context"
	"testing"
	"time"

	"github.com/mosinaRZ/finance-sync-backend/internal/domain/apperror"
	"github.com/mosinaRZ/finance-sync-backend/internal/domain/entity"
	"github.com/mosinaRZ/finance-sync-backend/internal/domain/repository"
)

// ---- fakes -----------------------------------------------------------------------------------

type fakeUsers struct {
	repository.UserRepository
	users       map[string]*entity.User
	deleted     map[string]bool
	roleUpdates map[string]entity.Role
	sessionBump map[string]int
}

func newFakeUsers(us ...*entity.User) *fakeUsers {
	f := &fakeUsers{users: map[string]*entity.User{}, deleted: map[string]bool{}, roleUpdates: map[string]entity.Role{}, sessionBump: map[string]int{}}
	for _, u := range us {
		f.users[u.ID] = u
	}
	return f
}

func (f *fakeUsers) FindByID(_ context.Context, id string) (*entity.User, error) {
	if u, ok := f.users[id]; ok {
		return u, nil
	}
	return nil, apperror.ErrNotFound("user not found")
}
func (f *fakeUsers) UpdateRole(_ context.Context, id string, r entity.Role) error {
	f.roleUpdates[id] = r
	f.users[id].Role = r
	return nil
}
func (f *fakeUsers) SetDeleted(_ context.Context, id string, d bool, _ time.Time) (uint64, error) {
	f.deleted[id] = d
	f.users[id].IsDeleted = d
	return 1, nil
}
func (f *fakeUsers) IncrementSessionVersion(_ context.Context, id string) (uint64, error) {
	f.sessionBump[id]++
	return 1, nil
}
func (f *fakeUsers) UpdateProfile(_ context.Context, id, first, last string, _ entity.Gender, _ string) error {
	f.users[id].FirstName, f.users[id].LastName = first, last
	return nil
}

type fakeRefresh struct {
	repository.RefreshTokenRepository
	revoked []string
}

func (f *fakeRefresh) RevokeAllForUser(_ context.Context, id string) error {
	f.revoked = append(f.revoked, id)
	return nil
}

type fakeQuery struct {
	repository.AdminQueryRepository
	users    *fakeUsers
	admins   int64
	overview repository.AdminOverview
}

func (f *fakeQuery) GetUser(ctx context.Context, id string, _ time.Duration) (*repository.AdminUserRow, error) {
	u, err := f.users.FindByID(ctx, id)
	if err != nil {
		return nil, err
	}
	return &repository.AdminUserRow{User: u}, nil
}
func (f *fakeQuery) CountActiveAdmins(context.Context) (int64, error) { return f.admins, nil }
func (f *fakeQuery) Overview(context.Context, time.Time, time.Time, int, time.Duration, int64) (repository.AdminOverview, error) {
	return f.overview, nil
}
func (f *fakeQuery) Labels(context.Context, []string) (map[string]string, error) {
	return map[string]string{}, nil
}

type fakeEvents struct {
	recorded []*entity.SecurityEvent
	between  []*entity.SecurityEvent
}

func (f *fakeEvents) Record(_ context.Context, e *entity.SecurityEvent) error {
	f.recorded = append(f.recorded, e)
	return nil
}
func (f *fakeEvents) List(context.Context, repository.SecurityEventFilter) ([]*entity.SecurityEvent, int64, error) {
	return nil, 0, nil
}
func (f *fakeEvents) Between(context.Context, time.Time, time.Time, repository.SecurityEventFilter, int) ([]*entity.SecurityEvent, error) {
	return f.between, nil
}

func newService(users *fakeUsers, admins int64) (*PanelService, *fakeEvents, *fakeRefresh) {
	ev := &fakeEvents{}
	rf := &fakeRefresh{}
	return NewPanelService(users, &fakeQuery{users: users, admins: admins}, ev, rf, nil), ev, rf
}

func code(t *testing.T, err error) apperror.Code {
	t.Helper()
	c, ok := apperror.CodeOf(err)
	if !ok {
		t.Fatalf("expected an app error, got %v", err)
	}
	return c
}

// ---- tests -----------------------------------------------------------------------------------

func TestAdminCannotDeleteOrRevokeSelf(t *testing.T) {
	users := newFakeUsers(&entity.User{ID: "a1", Role: entity.RoleAdmin})
	svc, ev, _ := newService(users, 2)
	if _, err := svc.DeleteUser(context.Background(), Actor{UserID: "a1"}, "a1"); code(t, err) != apperror.CodeForbidden {
		t.Fatalf("self delete must be forbidden, got %v", err)
	}
	if _, err := svc.RevokeSessions(context.Background(), Actor{UserID: "a1"}, "a1"); code(t, err) != apperror.CodeForbidden {
		t.Fatalf("self revoke must be forbidden, got %v", err)
	}
	if len(ev.recorded) != 0 || users.deleted["a1"] {
		t.Fatal("nothing may be recorded or changed on a refused action")
	}
}

func TestLastAdminCannotBeDeletedOrDemoted(t *testing.T) {
	users := newFakeUsers(&entity.User{ID: "a1", Role: entity.RoleAdmin}, &entity.User{ID: "a2", Role: entity.RoleAdmin})
	svc, _, _ := newService(users, 1)
	if _, err := svc.DeleteUser(context.Background(), Actor{UserID: "a2"}, "a1"); code(t, err) != apperror.CodeConflict {
		t.Fatalf("deleting the last admin must conflict, got %v", err)
	}
	user := entity.RoleUser
	if _, err := svc.UpdateUser(context.Background(), Actor{UserID: "a2"}, "a1", UpdateUserInput{Role: &user}); code(t, err) != apperror.CodeConflict {
		t.Fatalf("demoting the last admin must conflict, got %v", err)
	}
	// With a second admin it is allowed.
	svc2, _, _ := newService(users, 2)
	if _, err := svc2.UpdateUser(context.Background(), Actor{UserID: "a2"}, "a1", UpdateUserInput{Role: &user}); err != nil {
		t.Fatalf("demotion with another admin present should work: %v", err)
	}
	if users.roleUpdates["a1"] != entity.RoleUser {
		t.Fatal("role was not updated")
	}
}

func TestCannotChangeOwnRole(t *testing.T) {
	users := newFakeUsers(&entity.User{ID: "a1", Role: entity.RoleAdmin})
	svc, _, _ := newService(users, 3)
	r := entity.RolePro
	if _, err := svc.UpdateUser(context.Background(), Actor{UserID: "a1"}, "a1", UpdateUserInput{Role: &r}); code(t, err) != apperror.CodeForbidden {
		t.Fatalf("own role change must be forbidden, got %v", err)
	}
}

func TestDeleteIsSoftRevokesTokensAndIsRestorable(t *testing.T) {
	users := newFakeUsers(&entity.User{ID: "u1", Role: entity.RoleUser}, &entity.User{ID: "a1", Role: entity.RoleAdmin})
	svc, ev, rf := newService(users, 2)
	if _, err := svc.DeleteUser(context.Background(), Actor{UserID: "a1", IP: "1.2.3.4"}, "u1"); err != nil {
		t.Fatal(err)
	}
	if !users.deleted["u1"] || len(rf.revoked) != 1 || rf.revoked[0] != "u1" {
		t.Fatal("delete must soft-delete and revoke every refresh token")
	}
	if len(ev.recorded) != 1 || ev.recorded[0].Type != entity.SecAdminUserDeleted || ev.recorded[0].ActorUserID != "a1" || ev.recorded[0].UserID != "u1" {
		t.Fatalf("unexpected audit event: %+v", ev.recorded)
	}
	if _, err := svc.DeleteUser(context.Background(), Actor{UserID: "a1"}, "u1"); code(t, err) != apperror.CodeConflict {
		t.Fatal("double delete must conflict")
	}
	if _, err := svc.RestoreUser(context.Background(), Actor{UserID: "a1"}, "u1"); err != nil {
		t.Fatal(err)
	}
	if users.deleted["u1"] {
		t.Fatal("restore must clear the deleted flag")
	}
	if ev.recorded[len(ev.recorded)-1].Type != entity.SecAdminUserRestored {
		t.Fatal("restore must be audited")
	}
}

func TestEditNamesOnlyChangesWhatChanged(t *testing.T) {
	users := newFakeUsers(&entity.User{ID: "u1", Role: entity.RoleUser, FirstName: "Ali", LastName: "Ahdieh"})
	svc, ev, _ := newService(users, 1)
	newFirst := "  Reza\u200b  "
	if _, err := svc.UpdateUser(context.Background(), Actor{UserID: "a1"}, "u1", UpdateUserInput{FirstName: &newFirst}); err != nil {
		t.Fatal(err)
	}
	if users.users["u1"].FirstName != "Reza" || users.users["u1"].LastName != "Ahdieh" {
		t.Fatalf("names: %+v", users.users["u1"])
	}
	if len(ev.recorded) != 1 || ev.recorded[0].Detail != "first_name" {
		t.Fatalf("only the changed field should be audited: %+v", ev.recorded)
	}
	same := "Reza"
	if _, err := svc.UpdateUser(context.Background(), Actor{UserID: "a1"}, "u1", UpdateUserInput{FirstName: &same}); err != nil || len(ev.recorded) != 1 {
		t.Fatal("a no-op edit must not write an audit event")
	}
	tooLong := ""
	for i := 0; i < maxNameRunes+1; i++ {
		tooLong += "x"
	}
	if _, err := svc.UpdateUser(context.Background(), Actor{UserID: "a1"}, "u1", UpdateUserInput{LastName: &tooLong}); code(t, err) != apperror.CodeValidation {
		t.Fatal("over-long names must be rejected")
	}
}

func TestDeletedAccountCannotBeEdited(t *testing.T) {
	users := newFakeUsers(&entity.User{ID: "u1", Role: entity.RoleUser, IsDeleted: true})
	svc, _, _ := newService(users, 1)
	n := "x"
	if _, err := svc.UpdateUser(context.Background(), Actor{UserID: "a1"}, "u1", UpdateUserInput{FirstName: &n}); code(t, err) != apperror.CodeConflict {
		t.Fatal("editing a deleted account must conflict")
	}
}

func TestIsActiveAdminRereadsTheDatabase(t *testing.T) {
	users := newFakeUsers(
		&entity.User{ID: "admin", Role: entity.RoleAdmin},
		&entity.User{ID: "demoted", Role: entity.RoleUser},
		&entity.User{ID: "gone", Role: entity.RoleAdmin, IsDeleted: true},
	)
	svc, _, _ := newService(users, 1)
	for id, want := range map[string]bool{"admin": true, "demoted": false, "gone": false, "missing": false} {
		got, err := svc.IsActiveAdmin(context.Background(), id)
		if err != nil || got != want {
			t.Fatalf("IsActiveAdmin(%s) = %v, %v; want %v", id, got, err, want)
		}
	}
}

func TestAnalyticsFillsDaysCountsAndFilters(t *testing.T) {
	now := time.Now().UTC()
	mk := func(t entity.SecurityEventType, user, ip, platform string, at time.Time) *entity.SecurityEvent {
		return &entity.SecurityEvent{Type: t, UserID: user, IP: ip, Platform: platform, CreatedAt: at}
	}
	users := newFakeUsers()
	svc, ev, _ := newService(users, 1)
	ev.between = []*entity.SecurityEvent{
		mk(entity.SecLoginSuccess, "u1", "1.1.1.1", "android", now),
		mk(entity.SecLoginSuccess, "u2", "1.1.1.2", "android", now),
		mk(entity.SecLoginSuccess, "u1", "1.1.1.1", "ios", now),
		mk(entity.SecLoginFailed, "u3", "9.9.9.9", "android", now),
		mk(entity.SecLoginFailed, "u4", "9.9.9.9", "android", now),
		mk(entity.SecAccountLocked, "u3", "9.9.9.9", "android", now),
		mk(entity.SecAdminDenied, "u5", "8.8.8.8", "android", now),
	}
	a, err := svc.Analytics(context.Background(), AnalyticsQuery{Days: 7})
	if err != nil {
		t.Fatal(err)
	}
	if len(a.LoginsByDay) != 7 || len(a.SeverityByDay) != 7 {
		t.Fatalf("every day of the range must be present, got %d / %d", len(a.LoginsByDay), len(a.SeverityByDay))
	}
	if a.LoginsSuccess != 3 || a.LoginsFailed != 2 || a.UniqueLoginUsers != 2 {
		t.Fatalf("login counts wrong: %d/%d/%d", a.LoginsSuccess, a.LoginsFailed, a.UniqueLoginUsers)
	}
	if a.DangerEvents != 2 || a.LockedEvents != 1 { // locked + admin denied
		t.Fatalf("danger=%d locked=%d", a.DangerEvents, a.LockedEvents)
	}
	if len(a.Suspicious) == 0 || a.Suspicious[0].IP != "9.9.9.9" || a.Suspicious[0].Failed != 2 || a.Suspicious[0].Accounts != 2 {
		t.Fatalf("suspicious sources wrong: %+v", a.Suspicious)
	}
	var heat int64
	for _, row := range a.Heatmap {
		for _, v := range row {
			heat += v
		}
	}
	if heat != 3 {
		t.Fatalf("heatmap must hold the 3 successful sign-ins, got %d", heat)
	}
	last := a.LoginsByDay[len(a.LoginsByDay)-1]
	if last.Success != 3 || last.Failed != 2 {
		t.Fatalf("today's bucket wrong: %+v", last)
	}
	if a.Risk.Score <= 0 || a.Risk.Level == "" {
		t.Fatalf("risk not computed: %+v", a.Risk)
	}

	ios, err := svc.Analytics(context.Background(), AnalyticsQuery{Days: 7, Platform: "ios"})
	if err != nil {
		t.Fatal(err)
	}
	if ios.LoginsSuccess != 1 || ios.LoginsFailed != 0 {
		t.Fatalf("platform filter not applied: %d/%d", ios.LoginsSuccess, ios.LoginsFailed)
	}
}

func TestAnalyticsClampsRange(t *testing.T) {
	svc, _, _ := newService(newFakeUsers(), 1)
	a, err := svc.Analytics(context.Background(), AnalyticsQuery{Days: 100000, TzOffsetMin: 99999})
	if err != nil {
		t.Fatal(err)
	}
	if a.Days != maxAnalyticsDays || len(a.LoginsByDay) != maxAnalyticsDays {
		t.Fatalf("range must be clamped to %d, got %d", maxAnalyticsDays, a.Days)
	}
}

func TestRiskLevels(t *testing.T) {
	if r := computeRisk(&Analytics{LoginsSuccess: 100}); r.Level != "low" || r.Score != 0 {
		t.Fatalf("quiet period should be low risk: %+v", r)
	}
	if r := computeRisk(&Analytics{DangerEvents: 5, LockedEvents: 3, LoginsSuccess: 10, LoginsFailed: 10}); r.Level != "high" {
		t.Fatalf("bad period should be high risk: %+v", r)
	}
	if r := computeRisk(&Analytics{DangerEvents: 1000}); r.Score != 100 {
		t.Fatalf("score must be capped at 100: %+v", r)
	}
}

func TestSeverityClassification(t *testing.T) {
	cases := map[entity.SecurityEventType]string{
		entity.SecLoginSuccess:     entity.SeverityInfo,
		entity.SecLoginFailed:      entity.SeverityWarning,
		entity.SecAccountLocked:    entity.SeverityDanger,
		entity.SecAdminDenied:      entity.SeverityDanger,
		entity.SecAdminUserDeleted: entity.SeverityDanger,
		entity.SecPasswordChanged:  entity.SeverityWarning,
	}
	for typ, want := range cases {
		if got := entity.SeverityOf(typ); got != want {
			t.Fatalf("SeverityOf(%s) = %s, want %s", typ, got, want)
		}
	}
	for _, typ := range entity.AllSecurityEventTypes {
		if !entity.ValidSecurityEventType(string(typ)) {
			t.Fatalf("%s should be valid", typ)
		}
	}
	if entity.ValidSecurityEventType("drop table") {
		t.Fatal("unknown types must be rejected")
	}
}

func TestCleanName(t *testing.T) {
	cases := map[string]string{
		"  Ali   Reza ":  "Ali Reza",
		"A\u200bB":       "AB",
		"نیم\u200cفاصله": "نیم\u200cفاصله",
		"x\x00y":         "xy",
		"":               "",
	}
	for in, want := range cases {
		if got := cleanName(in); got != want {
			t.Fatalf("cleanName(%q) = %q, want %q", in, got, want)
		}
	}
}
