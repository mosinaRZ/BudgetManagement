package http

import (
	"net"
	"net/http"
	"time"

	"github.com/go-chi/chi/v5"

	"github.com/mosinaRZ/finance-sync-backend/internal/domain/entity"
	"github.com/mosinaRZ/finance-sync-backend/internal/domain/repository"
	"golang.org/x/time/rate"

	"github.com/mosinaRZ/finance-sync-backend/internal/interface/http/handler"
	"github.com/mosinaRZ/finance-sync-backend/internal/interface/http/middleware"
	"github.com/mosinaRZ/finance-sync-backend/internal/pkg/response"
)

// RouterDependencies contains all HTTP-layer dependencies created by startup.
// The router owns no application state and does not construct repositories or usecases.
type RouterDependencies struct {
	AuthHandler        *handler.AuthHandler
	SyncHandler        *handler.SyncHandler
	AdminHandler       *handler.AdminHandler
	DeviceHandler      *handler.DeviceHandler
	JWTSecret          string
	UserRepository     repository.UserRepository
	Env                string
	TrustedProxyCIDRs  []*net.IPNet
	CORSAllowedOrigins []string
	RateLimitStore     middleware.RateLimitStore
}

func NewRouter(deps RouterDependencies) *chi.Mux {
	r := chi.NewRouter()

	r.Use(middleware.RequestID)
	r.Use(middleware.Logging)
	r.Use(middleware.Recover)
	r.Use(middleware.SecurityHeaders(deps.Env))
	r.Use(middleware.CORS(deps.CORSAllowedOrigins))

	store := deps.RateLimitStore
	if store == nil {
		store = middleware.NewMemoryRateLimitStore()
	}
	publicRateLimiter := middleware.NewRateLimiterWithStoreAndProxies(store, rate.Every(3*time.Second), 20, deps.TrustedProxyCIDRs)
	protectedRateLimiter := middleware.NewRateLimiterWithStoreAndProxies(store, rate.Every(6*time.Second), 10, deps.TrustedProxyCIDRs)
	otpRateLimiter := middleware.NewRateLimiterWithStoreAndProxies(store, rate.Every(30*time.Second), 3, deps.TrustedProxyCIDRs)

	r.Group(func(r chi.Router) {
		r.Use(publicRateLimiter.LimitByIP)

		r.Get("/health", func(w http.ResponseWriter, _ *http.Request) {
			response.JSON(w, http.StatusOK, map[string]string{"status": "ok"})
		})

		if deps.AuthHandler != nil {
			r.With(otpRateLimiter.LimitByIP).Post("/auth/otp/request", deps.AuthHandler.RequestOTP)
			r.Post("/auth/register", deps.AuthHandler.Register)
			r.Post("/auth/login", deps.AuthHandler.Login)
			r.Post("/auth/refresh", deps.AuthHandler.Refresh)
			r.Post("/auth/recovery/prepare", deps.AuthHandler.PrepareRecovery)
			r.Post("/auth/password/reset", deps.AuthHandler.ResetPassword)
			r.Post("/auth/logout", deps.AuthHandler.Logout)
		}
	})

	r.Group(func(r chi.Router) {
		r.Use(middleware.Auth(deps.JWTSecret, deps.UserRepository))
		r.Use(protectedRateLimiter.LimitByUser)

		if deps.SyncHandler != nil {
			r.Post("/api/v1/sync", deps.SyncHandler.HandleSync)
		}
		if deps.DeviceHandler != nil {
			r.Get("/api/v1/devices", deps.DeviceHandler.List)
			r.Delete("/api/v1/devices/{deviceID}", deps.DeviceHandler.Revoke)
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
