package middleware

import (
	"context"
	"crypto/rand"
	"encoding/hex"
	"net/http"

	"github.com/mosinaRZ/finance-sync-backend/internal/interface/http/contextkeys"
)

const requestIDHeader = "X-Request-ID"

func RequestID(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		reqID := r.Header.Get(requestIDHeader)
		if reqID == "" {
			reqID = generateID()
		}

		ctx := contextkeys.WithRequestID(r.Context(), reqID)
		w.Header().Set(requestIDHeader, reqID)
		next.ServeHTTP(w, r.WithContext(ctx))
	})
}

func GetRequestID(ctx context.Context) string {
	if reqID, ok := contextkeys.RequestID(ctx); ok {
		return reqID
	}
	return "unknown"
}

func generateID() string {
	b := make([]byte, 16)
	_, _ = rand.Read(b)
	return hex.EncodeToString(b)
}
