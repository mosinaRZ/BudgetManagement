package handler

import (
	"context"
	"net/http"
	"net/http/httptest"
	"testing"
	"time"

	"github.com/mosinaRZ/finance-sync-backend/internal/domain/entity"
	"github.com/mosinaRZ/finance-sync-backend/internal/interface/http/contextkeys"
	deviceusecase "github.com/mosinaRZ/finance-sync-backend/internal/usecase/device"
	"github.com/mosinaRZ/finance-sync-backend/internal/usecase/mocks"
	"github.com/stretchr/testify/require"

	"github.com/go-chi/chi/v5"
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

func TestDeviceHandlerListFlagsPrimaryDevice(t *testing.T) {
	dev := &mocks.MockDeviceRepository{
		ListFunc: func(context.Context, string) ([]*entity.Device, error) {
			return []*entity.Device{
				{ID: "new", CreatedAt: time.Unix(200, 0), LastSeenAt: time.Unix(300, 0)},
				{ID: "first", CreatedAt: time.Unix(100, 0), LastSeenAt: time.Unix(250, 0)},
			}, nil
		},
		PrimaryFunc: func(context.Context, string) (*entity.Device, error) { return &entity.Device{ID: "first"}, nil },
	}
	h := NewDeviceHandler(deviceusecase.NewService(dev, deviceRefreshMock{}))
	req := httptest.NewRequest(http.MethodGet, "/api/v1/devices", nil)
	req = req.WithContext(contextkeys.WithUserID(req.Context(), "u1"))
	rec := httptest.NewRecorder()
	h.List(rec, req)
	require.Equal(t, http.StatusOK, rec.Code)
	body := rec.Body.String()
	require.Contains(t, body, `"id":"first"`)
	require.Contains(t, body, `"is_primary":true`)
	require.Contains(t, body, `"is_primary":false`)
}

func revokeRequest(callerDeviceID, targetDeviceID string) *http.Request {
	req := httptest.NewRequest(http.MethodDelete, "/api/v1/devices/"+targetDeviceID, nil)
	rctx := chi.NewRouteContext()
	rctx.URLParams.Add("deviceID", targetDeviceID)
	ctx := context.WithValue(req.Context(), chi.RouteCtxKey, rctx)
	ctx = contextkeys.WithUserID(ctx, "u1")
	if callerDeviceID != "" {
		ctx = contextkeys.WithDeviceID(ctx, callerDeviceID)
	}
	return req.WithContext(ctx)
}

func TestDeviceHandlerRevokeProtectsPrimaryDevice(t *testing.T) {
	deleted := false
	dev := &mocks.MockDeviceRepository{
		PrimaryFunc: func(context.Context, string) (*entity.Device, error) { return &entity.Device{ID: "first"}, nil },
		RevokeFunc:  func(context.Context, string, string) error { deleted = true; return nil },
	}
	h := NewDeviceHandler(deviceusecase.NewService(dev, deviceRefreshMock{}))

	rec := httptest.NewRecorder()
	h.Revoke(rec, revokeRequest("second", "first"))
	require.Equal(t, http.StatusForbidden, rec.Code)
	require.Contains(t, rec.Body.String(), "PRIMARY_DEVICE_PROTECTED")
	require.False(t, deleted)

	rec = httptest.NewRecorder()
	h.Revoke(rec, revokeRequest("first", "second"))
	require.Equal(t, http.StatusNoContent, rec.Code)
	require.True(t, deleted)
}

func TestDeviceHandlerRevokeRefusesNonPrimaryCallers(t *testing.T) {
	deleted := false
	dev := &mocks.MockDeviceRepository{
		PrimaryFunc: func(context.Context, string) (*entity.Device, error) { return &entity.Device{ID: "first"}, nil },
		RevokeFunc:  func(context.Context, string, string) error { deleted = true; return nil },
	}
	h := NewDeviceHandler(deviceusecase.NewService(dev, deviceRefreshMock{}))

	// A secondary device may not remove another secondary device...
	rec := httptest.NewRecorder()
	h.Revoke(rec, revokeRequest("second", "third"))
	require.Equal(t, http.StatusForbidden, rec.Code)
	require.Contains(t, rec.Body.String(), "DEVICE_REMOVAL_FORBIDDEN")
	require.False(t, deleted)

	// ...nor can a caller that cannot be identified (legacy token).
	rec = httptest.NewRecorder()
	h.Revoke(rec, revokeRequest("", "third"))
	require.Equal(t, http.StatusForbidden, rec.Code)
	require.False(t, deleted)

	// Every device may still sign itself out.
	rec = httptest.NewRecorder()
	h.Revoke(rec, revokeRequest("second", "second"))
	require.Equal(t, http.StatusNoContent, rec.Code)
	require.True(t, deleted)
}

func eventsRequest(rawQuery, callerDeviceID string) *http.Request {
	req := httptest.NewRequest(http.MethodGet, "/api/v1/devices/events"+rawQuery, nil)
	ctx := contextkeys.WithUserID(req.Context(), "u1")
	if callerDeviceID != "" {
		ctx = contextkeys.WithDeviceID(ctx, callerDeviceID)
	}
	return req.WithContext(ctx)
}

func TestDeviceHandlerEventsReturnsActivityAfterTheCallerJoined(t *testing.T) {
	joined := time.UnixMilli(1_000_000)
	dev := &mocks.MockDeviceRepository{
		ListFunc: func(context.Context, string) ([]*entity.Device, error) {
			return []*entity.Device{{ID: "me", CreatedAt: joined}}, nil
		},
	}
	events := &mocks.MockDeviceEventRepository{
		ListAfterFunc: func(_ context.Context, _ string, after time.Time, _ int) ([]*entity.DeviceEvent, error) {
			require.True(t, after.Equal(joined), "the feed must start at the moment the caller joined")
			return []*entity.DeviceEvent{{
				ID: "e1", Type: entity.DeviceEventSignedIn, DeviceID: "new", DeviceName: "Pixel 8",
				ActorDeviceID: "new", ActorName: "Pixel 8", CreatedAt: time.UnixMilli(2_000_000),
			}}, nil
		},
	}
	svc := deviceusecase.NewService(dev, deviceRefreshMock{})
	svc.SetEventRepository(events)
	h := NewDeviceHandler(svc)

	rec := httptest.NewRecorder()
	h.Events(rec, eventsRequest("?after=0", "me"))
	require.Equal(t, http.StatusOK, rec.Code)
	body := rec.Body.String()
	require.Contains(t, body, `"type":"signed_in"`)
	require.Contains(t, body, `"device_name":"Pixel 8"`)
	require.Contains(t, body, `"created_at_ms":2000000`)
	require.Contains(t, body, `"cursor_ms":2000000`)
}

func TestDeviceHandlerEventsRejectsMalformedCursor(t *testing.T) {
	h := NewDeviceHandler(deviceusecase.NewService(&mocks.MockDeviceRepository{}, deviceRefreshMock{}))
	for _, query := range []string{"?after=abc", "?after=-5"} {
		rec := httptest.NewRecorder()
		h.Events(rec, eventsRequest(query, "me"))
		require.Equal(t, http.StatusBadRequest, rec.Code, query)
	}
}

func TestDeviceHandlerEventsIsEmptyWithoutAFeed(t *testing.T) {
	h := NewDeviceHandler(deviceusecase.NewService(&mocks.MockDeviceRepository{}, deviceRefreshMock{}))
	rec := httptest.NewRecorder()
	h.Events(rec, eventsRequest("", "me"))
	require.Equal(t, http.StatusOK, rec.Code)
	require.Contains(t, rec.Body.String(), `"events":[]`)
	require.Contains(t, rec.Body.String(), `"cursor_ms":0`)
}
