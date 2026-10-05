package middleware

import (
	"net"
	"net/http"

	"github.com/mosinaRZ/finance-sync-backend/internal/interface/http/contextkeys"
)

// ClientIPContext resolves the client address once (honouring forwarding headers
// only from trusted proxies) and exposes it to handlers through the request context.
func ClientIPContext(trusted []*net.IPNet) func(http.Handler) http.Handler {
	return func(next http.Handler) http.Handler {
		return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			ctx := contextkeys.WithClientIP(r.Context(), ClientIP(r, trusted))
			next.ServeHTTP(w, r.WithContext(ctx))
		})
	}
}
