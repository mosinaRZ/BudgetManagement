package middleware

import (
	"net/http"

	"github.com/mosinaRZ/finance-sync-backend/internal/domain/apperror"
	"github.com/mosinaRZ/finance-sync-backend/internal/domain/entity"
	"github.com/mosinaRZ/finance-sync-backend/internal/interface/http/contextkeys"
	"github.com/mosinaRZ/finance-sync-backend/internal/pkg/response"
)

// RequireRoles allows an endpoint to be accessed only by one of the supplied roles.
// Authentication must run before this middleware so the role comes from a validated JWT.
func RequireRoles(roles ...entity.Role) func(http.Handler) http.Handler {
	allowed := make(map[entity.Role]struct{}, len(roles))
	for _, role := range roles {
		if role.Valid() {
			allowed[role] = struct{}{}
		}
	}
	return func(next http.Handler) http.Handler {
		return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			value, ok := contextkeys.Role(r.Context())
			if !ok {
				response.Error(w, apperror.ErrForbidden("insufficient permissions"))
				return
			}
			if _, ok := allowed[entity.Role(value)]; !ok {
				response.Error(w, apperror.ErrForbidden("insufficient permissions"))
				return
			}
			next.ServeHTTP(w, r)
		})
	}
}
