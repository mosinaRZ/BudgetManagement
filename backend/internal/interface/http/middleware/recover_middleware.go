package middleware

import (
	"log/slog"
	"net/http"
	"runtime/debug"

	"github.com/mosinaRZ/finance-sync-backend/internal/domain/apperror"
	"github.com/mosinaRZ/finance-sync-backend/internal/pkg/response"
)

func Recover(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		defer func() {
			if rec := recover(); rec != nil {
				reqID := GetRequestID(r.Context())
				stack := debug.Stack()

				slog.Error("Panic recovered",
					slog.String("request_id", reqID),
					slog.Any("panic", rec),
					slog.String("stacktrace", string(stack)),
				)

				// استفاده از خطای عمومی برای عدم افشای اطلاعات حساس
				err := apperror.ErrInternal("Internal server error")
				response.Error(w, err)
			}
		}()

		next.ServeHTTP(w, r)
	})
}
