package handler

import (
	"context"
	"github.com/mosinaRZ/finance-sync-backend/internal/domain/entity"
	"github.com/mosinaRZ/finance-sync-backend/internal/interface/http/contextkeys"
	deviceusecase "github.com/mosinaRZ/finance-sync-backend/internal/usecase/device"
	"github.com/mosinaRZ/finance-sync-backend/internal/usecase/mocks"
	"github.com/stretchr/testify/require"
	"net/http"
	"net/http/httptest"
	"testing"
	"time"
)

type deviceRefreshMock struct{}

func (deviceRefreshMock) Create(context.Context, *entity.RefreshToken) error { return nil }
func (deviceRefreshMock) FindByHash(context.Context, string) (*entity.RefreshToken, error) {
	return nil, nil
}
func (deviceRefreshMock) Revoke(context.Context, string) error                        { return nil }
func (deviceRefreshMock) RevokeAllForUser(context.Context, string) error              { return nil }
func (deviceRefreshMock) RevokeByUserAndDevice(context.Context, string, string) error { return nil }

func TestDeviceHandlerListUsesAuthenticatedContextUser(t *testing.T) {
	dev := &mocks.MockDeviceRepository{ListFunc: func(_ context.Context, id string) ([]*entity.Device, error) {
		if id != "u1" {
			t.Fatalf("unexpected user %q", id)
		}
		return []*entity.Device{{ID: "d1", LastSeenAt: time.Unix(100, 0), CreatedAt: time.Unix(90, 0)}}, nil
	}}
	h := NewDeviceHandler(deviceusecase.NewService(dev, deviceRefreshMock{}))
	req := httptest.NewRequest(http.MethodGet, "/api/v1/devices", nil)
	req = req.WithContext(contextkeys.WithUserID(req.Context(), "u1"))
	rec := httptest.NewRecorder()
	h.List(rec, req)
	require.Equal(t, http.StatusOK, rec.Code)
	require.Contains(t, rec.Body.String(), `"id":"d1"`)
}
