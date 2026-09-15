package middleware

import (
	"context"
	"net/http"
	"strings"

	"github.com/mosinaRZ/finance-sync-backend/internal/domain/apperror"
	"github.com/mosinaRZ/finance-sync-backend/internal/infrastructure/auth"
	"github.com/mosinaRZ/finance-sync-backend/internal/interface/http/contextkeys"
	"github.com/mosinaRZ/finance-sync-backend/internal/pkg/response"
)

func Auth(secret string) func(http.Handler) http.Handler {
	return func(next http.Handler) http.Handler {
		return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			authHeader := r.Header.Get("Authorization")
			if authHeader == "" || !strings.HasPrefix(authHeader, "Bearer ") {
				response.Error(w, apperror.ErrUnauthorized("Missing or invalid Authorization header"))
				return
			}

			tokenString := strings.TrimPrefix(authHeader, "Bearer ")
			userID, err := auth.ParseAndValidateAccessToken(tokenString, secret)
			if err != nil {
				response.Error(w, apperror.ErrUnauthorized("Invalid or expired token"))
				return
			}

			ctx := contextkeys.WithUserID(r.Context(), userID)
			next.ServeHTTP(w, r.WithContext(ctx))
		})
	}
}

func UserIDFromContext(ctx context.Context) (string, bool) {
	return contextkeys.UserID(ctx)
}
