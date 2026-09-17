package http

import (
	"net/http"
	"time"

	"github.com/go-chi/chi/v5"

	"github.com/mosinaRZ/finance-sync-backend/internal/domain/entity"
	"golang.org/x/time/rate"

	"github.com/mosinaRZ/finance-sync-backend/internal/interface/http/handler"
	"github.com/mosinaRZ/finance-sync-backend/internal/interface/http/middleware"
	"github.com/mosinaRZ/finance-sync-backend/internal/pkg/response"
)

// RouterDependencies contains all HTTP-layer dependencies created by startup.
// The router owns no application state and does not construct repositories or usecases.
type RouterDependencies struct {
	AuthHandler  *handler.AuthHandler
	SyncHandler  *handler.SyncHandler
	AdminHandler *handler.AdminHandler
	JWTSecret    string
}

func NewRouter(deps RouterDependencies) *chi.Mux {
	r := chi.NewRouter()

	r.Use(middleware.RequestID)
	r.Use(middleware.Logging)
	r.Use(middleware.Recover)

	publicRateLimiter := middleware.NewRateLimiter(rate.Every(3*time.Second), 20)
	protectedRateLimiter := middleware.NewRateLimiter(rate.Every(6*time.Second), 10)

	r.Group(func(r chi.Router) {
		r.Use(publicRateLimiter.LimitByIP)

		r.Get("/health", func(w http.ResponseWriter, _ *http.Request) {
			response.JSON(w, http.StatusOK, map[string]string{"status": "ok"})
		})

		if deps.AuthHandler != nil {
			r.Post("/auth/otp/request", deps.AuthHandler.RequestOTP)
			r.Post("/auth/register", deps.AuthHandler.Register)
			r.Post("/auth/login", deps.AuthHandler.Login)
			r.Post("/auth/refresh", deps.AuthHandler.Refresh)
			r.Post("/auth/password/reset", deps.AuthHandler.ResetPassword)
			r.Post("/auth/logout", deps.AuthHandler.Logout)
		}
	})

	r.Group(func(r chi.Router) {
		r.Use(middleware.Auth(deps.JWTSecret))
		r.Use(protectedRateLimiter.LimitByUser)

		if deps.SyncHandler != nil {
			r.Post("/api/v1/sync", deps.SyncHandler.HandleSync)
		}
		if deps.AdminHandler != nil {
			r.Route("/api/v1/admin", func(r chi.Router) {
				r.Use(middleware.RequireRoles(entity.RoleAdmin))
				r.Put("/users/{userID}/role", deps.AdminHandler.UpdateUserRole)
			})
		}
	})

	return r
}
