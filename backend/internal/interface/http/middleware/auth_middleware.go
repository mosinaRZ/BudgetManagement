package middleware

import (
	"context"
	"net/http"
	"strings"

	"github.com/mosinaRZ/finance-sync-backend/internal/domain/apperror"
	"github.com/mosinaRZ/finance-sync-backend/internal/domain/repository"
	"github.com/mosinaRZ/finance-sync-backend/internal/infrastructure/auth"
	"github.com/mosinaRZ/finance-sync-backend/internal/interface/http/contextkeys"
	"github.com/mosinaRZ/finance-sync-backend/internal/pkg/response"
)

func Auth(secret string, users repository.UserRepository) func(http.Handler) http.Handler {
	return AuthWithDevices(secret, users, nil)
}

// AuthWithDevices behaves like Auth and additionally rejects access tokens whose device has
// been removed from the account. Without this, a removed device would keep working until its
// short-lived access token expired. When devices is nil the extra check is skipped.
func AuthWithDevices(secret string, users repository.UserRepository, devices repository.DeviceRepository) func(http.Handler) http.Handler {
	return func(next http.Handler) http.Handler {
		return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			authHeader := r.Header.Get("Authorization")
			if authHeader == "" || !strings.HasPrefix(authHeader, "Bearer ") {
				response.Error(w, apperror.ErrUnauthorized("Missing or invalid Authorization header"))
				return
			}

			tokenString := strings.TrimPrefix(authHeader, "Bearer ")
			claims, err := auth.ParseAndValidateAccessTokenClaims(tokenString, secret)
			if err != nil {
				response.Error(w, apperror.ErrUnauthorized("Invalid or expired token"))
				return
			}

			if users == nil {
				response.Error(w, apperror.ErrInternal("authentication service is not configured"))
				return
			}
			version, err := users.GetSessionVersion(r.Context(), claims.UserID)
			if err != nil || version != claims.SessionVersion {
				response.Error(w, apperror.ErrUnauthorized("Invalid or expired token"))
				return
			}

			if devices != nil && claims.DeviceID != "" {
				registered, err := devices.Exists(r.Context(), claims.UserID, claims.DeviceID)
				if err != nil {
					// A storage failure must never look like "device removed": that would sign
					// the user out on a transient outage. Surface it as a retryable server error.
					response.Error(w, apperror.ErrInternal("failed to verify device"))
					return
				}
				if !registered {
					response.Error(w, apperror.ErrUnauthorized("This device has been removed from the account"))
					return
				}
			}

			ctx := contextkeys.WithUserID(r.Context(), claims.UserID)
			if claims.DeviceID != "" {
				ctx = contextkeys.WithDeviceID(ctx, claims.DeviceID)
			}
			ctx = contextkeys.WithRole(ctx, string(claims.Role))
			next.ServeHTTP(w, r.WithContext(ctx))
		})
	}
}

func UserIDFromContext(ctx context.Context) (string, bool) {
	return contextkeys.UserID(ctx)
}
