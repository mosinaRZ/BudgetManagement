package middleware

import (
	"log/slog"
	"net/http"
	"time"

	"github.com/mosinaRZ/finance-sync-backend/internal/interface/http/contextkeys"
)

// statusWriter برای رهگیری HTTP Status Code
type statusWriter struct {
	http.ResponseWriter
	status int
}

func (w *statusWriter) WriteHeader(statusCode int) {
	w.status = statusCode
	w.ResponseWriter.WriteHeader(statusCode)
}

func Logging(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		start := time.Now()

		sw := &statusWriter{
			ResponseWriter: w,
			status:         http.StatusOK, // پیش‌فرض اگر WriteHeader صدا زده نشود
		}

		next.ServeHTTP(sw, r)

		latency := time.Since(start)
		reqID := GetRequestID(r.Context())

		slog.Info("HTTP Request",
			slog.String("request_id", reqID),
			slog.String("method", r.Method),
			slog.String("path", r.URL.Path),
			slog.Int("status", sw.status),
			slog.Duration("latency", latency),
			slog.String("ip", func() string {
				if ip, ok := contextkeys.ClientIP(r.Context()); ok {
					return ip
				}
				return ClientIP(r, nil)
			}()),
		)
	})
}
