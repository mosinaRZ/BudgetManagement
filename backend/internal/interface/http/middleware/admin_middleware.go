package middleware

import (
	"context"
	"net/http"
	"sync"
	"time"

	"github.com/mosinaRZ/finance-sync-backend/internal/domain/apperror"
	"github.com/mosinaRZ/finance-sync-backend/internal/domain/repository"
	"github.com/mosinaRZ/finance-sync-backend/internal/interface/http/contextkeys"
	"github.com/mosinaRZ/finance-sync-backend/internal/pkg/response"
	adminusecase "github.com/mosinaRZ/finance-sync-backend/internal/usecase/admin"
)

// AdminGate decides whether the caller may use the admin console.
type AdminGate interface {
	IsActiveAdmin(ctx context.Context, userID string) (bool, error)
	RecordDenied(ctx context.Context, a adminusecase.Actor, path string)
}

// RequireActiveAdmin re-checks the caller's role against the database on every request. The role
// claim in the access token is only a hint: an administrator who was demoted or deleted keeps a
// valid token until it expires, and that must not keep the console open. Refused attempts are
// written to the security trail.
func RequireActiveAdmin(gate AdminGate) func(http.Handler) http.Handler {
	return func(next http.Handler) http.Handler {
		return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			userID, ok := contextkeys.UserID(r.Context())
			if !ok || userID == "" {
				response.Error(w, apperror.ErrUnauthorized("authentication required"))
				return
			}
			if gate == nil {
				response.Error(w, apperror.ErrForbidden("insufficient permissions"))
				return
			}
			admin, err := gate.IsActiveAdmin(r.Context(), userID)
			if err != nil {
				response.Error(w, err)
				return
			}
			if !admin {
				deviceID, _ := contextkeys.DeviceID(r.Context())
				ip, _ := contextkeys.ClientIP(r.Context())
				gate.RecordDenied(r.Context(), adminusecase.Actor{UserID: userID, DeviceID: deviceID, IP: ip}, r.URL.Path)
				response.Error(w, apperror.ErrForbidden("insufficient permissions"))
				return
			}
			next.ServeHTTP(w, r)
		})
	}
}

type presenceKey struct{ user, device string }

// Presence refreshes the calling device's last-seen time so the admin console can tell who is
// online. It is throttled per device (a poll every few seconds must not become a write every few
// seconds) and runs the write in the background so it never slows a request down.
func Presence(devices repository.DeviceRepository, every time.Duration) func(http.Handler) http.Handler {
	if every <= 0 {
		every = 45 * time.Second
	}
	var mu sync.Mutex
	last := map[presenceKey]time.Time{}
	return func(next http.Handler) http.Handler {
		return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			if devices != nil {
				userID, okU := contextkeys.UserID(r.Context())
				deviceID, okD := contextkeys.DeviceID(r.Context())
				if okU && okD {
					key := presenceKey{userID, deviceID}
					now := time.Now()
					mu.Lock()
					due := now.Sub(last[key]) >= every
					if due {
						last[key] = now
						if len(last) > 20000 {
							// Bound memory: forgetting entries only means a few extra writes.
							last = map[presenceKey]time.Time{key: now}
						}
					}
					mu.Unlock()
					if due {
						go func() {
							ctx, cancel := context.WithTimeout(context.Background(), 3*time.Second)
							defer cancel()
							_ = devices.Touch(ctx, userID, deviceID)
						}()
					}
				}
			}
			next.ServeHTTP(w, r)
		})
	}
}
