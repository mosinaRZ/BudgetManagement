package device

import (
	"context"
	"github.com/mosinaRZ/finance-sync-backend/internal/domain/entity"
	"github.com/mosinaRZ/finance-sync-backend/internal/usecase/mocks"
	"testing"
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

func TestRevokeRevokesDeviceAndRefreshTokens(t *testing.T) {
	dev := &mocks.MockDeviceRepository{RevokeFunc: func(_ context.Context, u, d string) error {
		if u != "u1" || d != "d1" {
			t.Fatalf("unexpected %s/%s", u, d)
		}
		return nil
	}}
	ref := &refreshMock{}
	if err := NewService(dev, ref).Revoke(context.Background(), "u1", "d1"); err != nil {
		t.Fatal(err)
	}
	if !ref.revoked {
		t.Fatal("refresh tokens were not revoked")
	}
}
