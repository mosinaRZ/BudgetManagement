package device

import (
	"context"
	"testing"
	"time"

	"github.com/mosinaRZ/finance-sync-backend/internal/domain/apperror"
	"github.com/mosinaRZ/finance-sync-backend/internal/domain/entity"
	"github.com/mosinaRZ/finance-sync-backend/internal/usecase/mocks"
)

type refreshMock struct{ revoked bool }

func (m *refreshMock) Create(context.Context, *entity.RefreshToken) error { return nil }
func (m *refreshMock) FindByHash(context.Context, string) (*entity.RefreshToken, error) {
	return nil, nil
}
func (m *refreshMock) Revoke(context.Context, string) error           { return nil }
func (m *refreshMock) RevokeAllForUser(context.Context, string) error { return nil }
func (m *refreshMock) RevokeByUserAndDevice(_ context.Context, u, d string) error {
	m.revoked = true
	return nil
}

func primaryRepo(primaryID string, revoked *[]string) *mocks.MockDeviceRepository {
	return &mocks.MockDeviceRepository{
		PrimaryFunc: func(context.Context, string) (*entity.Device, error) {
			return &entity.Device{ID: primaryID}, nil
		},
		RevokeFunc: func(_ context.Context, _, d string) error {
			*revoked = append(*revoked, d)
			return nil
		},
	}
}

func TestRevokeRevokesDeviceAndRefreshTokens(t *testing.T) {
	var revoked []string
	dev := primaryRepo("primary", &revoked)
	ref := &refreshMock{}
	if err := NewService(dev, ref).Revoke(context.Background(), "u1", "primary", "d1"); err != nil {
		t.Fatal(err)
	}
	if len(revoked) != 1 || revoked[0] != "d1" {
		t.Fatalf("revoked = %v, want [d1]", revoked)
	}
	if !ref.revoked {
		t.Fatal("refresh tokens were not revoked")
	}
}

func TestPrimaryDeviceCannotBeRemovedByAnotherDevice(t *testing.T) {
	var revoked []string
	ref := &refreshMock{}
	svc := NewService(primaryRepo("primary", &revoked), ref)

	for _, caller := range []string{"other-device", ""} { // "" = legacy token, caller unknown
		err := svc.Revoke(context.Background(), "u1", caller, "primary")
		if code, ok := apperror.CodeOf(err); !ok || code != apperror.CodePrimaryDeviceProtected {
			t.Fatalf("caller %q: err = %v, want PRIMARY_DEVICE_PROTECTED", caller, err)
		}
	}
	if len(revoked) != 0 || ref.revoked {
		t.Fatalf("primary device was removed: revoked=%v refresh=%v", revoked, ref.revoked)
	}
}

func TestPrimaryDeviceCanRemoveOtherDevices(t *testing.T) {
	var revoked []string
	svc := NewService(primaryRepo("primary", &revoked), &refreshMock{})
	if err := svc.Revoke(context.Background(), "u1", "primary", "second"); err != nil {
		t.Fatal(err)
	}
	if len(revoked) != 1 || revoked[0] != "second" {
		t.Fatalf("revoked = %v, want [second]", revoked)
	}
}

func TestNonPrimaryDeviceCannotRemoveOtherDevices(t *testing.T) {
	var revoked []string
	ref := &refreshMock{}
	svc := NewService(primaryRepo("primary", &revoked), ref)

	// A secondary device must not be able to remove another secondary device, nor a caller
	// that cannot be identified (legacy token).
	for _, caller := range []string{"second", ""} {
		err := svc.Revoke(context.Background(), "u1", caller, "third")
		if code, ok := apperror.CodeOf(err); !ok || code != apperror.CodeDeviceRemovalForbidden {
			t.Fatalf("caller %q: err = %v, want DEVICE_REMOVAL_FORBIDDEN", caller, err)
		}
	}
	if len(revoked) != 0 || ref.revoked {
		t.Fatalf("a device was removed: revoked=%v refresh=%v", revoked, ref.revoked)
	}
}

func TestAnyDeviceCanRemoveItself(t *testing.T) {
	var revoked []string
	svc := NewService(primaryRepo("primary", &revoked), &refreshMock{})
	if err := svc.Revoke(context.Background(), "u1", "second", "second"); err != nil {
		t.Fatal(err)
	}
	if len(revoked) != 1 || revoked[0] != "second" {
		t.Fatalf("revoked = %v, want [second]", revoked)
	}
}

type recordingEvents struct {
	recorded []*entity.DeviceEvent
	stored   []*entity.DeviceEvent
}

func (r *recordingEvents) repo() *mocks.MockDeviceEventRepository {
	return &mocks.MockDeviceEventRepository{
		RecordFunc: func(_ context.Context, e *entity.DeviceEvent) error {
			r.recorded = append(r.recorded, e)
			return nil
		},
		ListAfterFunc: func(_ context.Context, _ string, after time.Time, _ int) ([]*entity.DeviceEvent, error) {
			var out []*entity.DeviceEvent
			for _, e := range r.stored {
				if e.CreatedAt.After(after) {
					out = append(out, e)
				}
			}
			return out, nil
		},
	}
}

func TestRevokeRecordsRemovalEventWithDeviceNames(t *testing.T) {
	var revoked []string
	dev := primaryRepo("primary", &revoked)
	dev.ListFunc = func(context.Context, string) ([]*entity.Device, error) {
		return []*entity.Device{
			{ID: "primary", Name: "Pixel 8"},
			{ID: "second", Name: "Galaxy S24", Platform: "android"},
		}, nil
	}
	events := &recordingEvents{}
	svc := NewService(dev, &refreshMock{})
	svc.SetEventRepository(events.repo())

	if err := svc.Revoke(context.Background(), "u1", "primary", "second"); err != nil {
		t.Fatal(err)
	}
	if len(events.recorded) != 1 {
		t.Fatalf("recorded %d events, want 1", len(events.recorded))
	}
	got := events.recorded[0]
	if got.Type != entity.DeviceEventRemoved || got.DeviceID != "second" || got.DeviceName != "Galaxy S24" ||
		got.ActorDeviceID != "primary" || got.ActorName != "Pixel 8" || got.Platform != "android" {
		t.Fatalf("unexpected event: %+v", got)
	}
}

func TestRefusedRevokeRecordsNoEvent(t *testing.T) {
	var revoked []string
	events := &recordingEvents{}
	svc := NewService(primaryRepo("primary", &revoked), &refreshMock{})
	svc.SetEventRepository(events.repo())

	_ = svc.Revoke(context.Background(), "u1", "second", "third")
	_ = svc.Revoke(context.Background(), "u1", "second", "primary")
	if len(events.recorded) != 0 {
		t.Fatalf("refused removals recorded events: %+v", events.recorded)
	}
}

func TestSelfRemovalRecordsNoEvent(t *testing.T) {
	var revoked []string
	events := &recordingEvents{}
	svc := NewService(primaryRepo("primary", &revoked), &refreshMock{})
	svc.SetEventRepository(events.repo())

	if err := svc.Revoke(context.Background(), "u1", "second", "second"); err != nil {
		t.Fatal(err)
	}
	if len(events.recorded) != 0 {
		t.Fatalf("self removal recorded events: %+v", events.recorded)
	}
}

func TestEventsOnlyCoverActivityAfterTheCallerJoinedAndNeverItsOwn(t *testing.T) {
	base := time.Unix(10_000, 0)
	dev := &mocks.MockDeviceRepository{
		ListFunc: func(context.Context, string) ([]*entity.Device, error) {
			return []*entity.Device{
				{ID: "primary", CreatedAt: base},
				{ID: "me", CreatedAt: base.Add(time.Hour)},
			}, nil
		},
	}
	events := &recordingEvents{stored: []*entity.DeviceEvent{
		{ID: "before-join", Type: entity.DeviceEventSignedIn, DeviceID: "x", CreatedAt: base.Add(30 * time.Minute)},
		{ID: "my-sign-in", Type: entity.DeviceEventSignedIn, DeviceID: "me", ActorDeviceID: "me", CreatedAt: base.Add(time.Hour)},
		{ID: "someone-signed-in", Type: entity.DeviceEventSignedIn, DeviceID: "third", ActorDeviceID: "third", CreatedAt: base.Add(2 * time.Hour)},
		{ID: "i-removed", Type: entity.DeviceEventRemoved, DeviceID: "fourth", ActorDeviceID: "me", CreatedAt: base.Add(3 * time.Hour)},
		{ID: "third-removed", Type: entity.DeviceEventRemoved, DeviceID: "third", ActorDeviceID: "primary", CreatedAt: base.Add(4 * time.Hour)},
	}}
	svc := NewService(dev, &refreshMock{})
	svc.SetEventRepository(events.repo())

	feed, err := svc.Events(context.Background(), "u1", "me", time.Time{})
	if err != nil {
		t.Fatal(err)
	}
	var ids []string
	for _, e := range feed.Events {
		ids = append(ids, e.ID)
	}
	if len(ids) != 2 || ids[0] != "someone-signed-in" || ids[1] != "third-removed" {
		t.Fatalf("events = %v, want [someone-signed-in third-removed]", ids)
	}
	if !feed.Cursor.Equal(base.Add(4 * time.Hour)) {
		t.Fatalf("cursor = %v, want %v", feed.Cursor, base.Add(4*time.Hour))
	}
}

func TestEventsCursorAdvancesEvenWhenEveryEntryIsFiltered(t *testing.T) {
	base := time.Unix(10_000, 0)
	dev := &mocks.MockDeviceRepository{
		ListFunc: func(context.Context, string) ([]*entity.Device, error) {
			return []*entity.Device{{ID: "me", CreatedAt: base}}, nil
		},
	}
	events := &recordingEvents{stored: []*entity.DeviceEvent{
		{ID: "mine", Type: entity.DeviceEventRemoved, DeviceID: "x", ActorDeviceID: "me", CreatedAt: base.Add(time.Minute)},
	}}
	svc := NewService(dev, &refreshMock{})
	svc.SetEventRepository(events.repo())

	feed, err := svc.Events(context.Background(), "u1", "me", time.Time{})
	if err != nil {
		t.Fatal(err)
	}
	if len(feed.Events) != 0 || !feed.Cursor.Equal(base.Add(time.Minute)) {
		t.Fatalf("feed = %+v, want no events and the cursor moved past the filtered entry", feed)
	}
}

func TestEventsIsEmptyForUnknownCallers(t *testing.T) {
	dev := &mocks.MockDeviceRepository{
		ListFunc: func(context.Context, string) ([]*entity.Device, error) {
			return []*entity.Device{{ID: "primary", CreatedAt: time.Unix(1, 0)}}, nil
		},
	}
	events := &recordingEvents{stored: []*entity.DeviceEvent{
		{ID: "e", Type: entity.DeviceEventSignedIn, DeviceID: "x", CreatedAt: time.Unix(100, 0)},
	}}
	svc := NewService(dev, &refreshMock{})
	svc.SetEventRepository(events.repo())

	for _, caller := range []string{"", "not-registered"} {
		feed, err := svc.Events(context.Background(), "u1", caller, time.Time{})
		if err != nil || len(feed.Events) != 0 {
			t.Fatalf("caller %q: feed=%+v err=%v, want empty", caller, feed, err)
		}
	}
}

func TestOverviewMarksEarliestRegisteredDeviceAsPrimary(t *testing.T) {
	base := time.Unix(1000, 0)
	dev := &mocks.MockDeviceRepository{
		ListFunc: func(context.Context, string) ([]*entity.Device, error) {
			return []*entity.Device{
				{ID: "newer", CreatedAt: base.Add(time.Hour)},
				{ID: "oldest", CreatedAt: base},
			}, nil
		},
		// No PrimaryFunc: exercises the fallback computed from the listed devices.
	}
	got, err := NewService(dev, &refreshMock{}).Overview(context.Background(), "u1")
	if err != nil {
		t.Fatal(err)
	}
	if got.PrimaryID != "oldest" {
		t.Fatalf("primary = %q, want oldest", got.PrimaryID)
	}
}
