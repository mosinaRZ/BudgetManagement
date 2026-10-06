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

func TestNonPrimaryDeviceCanRemoveAnotherNonPrimaryDevice(t *testing.T) {
	var revoked []string
	svc := NewService(primaryRepo("primary", &revoked), &refreshMock{})
	if err := svc.Revoke(context.Background(), "u1", "second", "third"); err != nil {
		t.Fatal(err)
	}
	if len(revoked) != 1 || revoked[0] != "third" {
		t.Fatalf("revoked = %v, want [third]", revoked)
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
