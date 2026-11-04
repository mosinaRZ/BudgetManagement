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
	AuthHandler       *handler.AuthHandler
	SyncHandler       *handler.SyncHandler
	AdminHandler      *handler.AdminHandler
	AdminPanelHandler *handler.AdminPanelHandler
	// AdminGate re-checks the caller's role in the database for every admin request.
	AdminGate          middleware.AdminGate
	DeviceHandler      *handler.DeviceHandler
	LegalHandler       *handler.LegalHandler
	JWTSecret          string
	UserRepository     repository.UserRepository
	DeviceRepository   repository.DeviceRepository
	Env                string
	TrustedProxyCIDRs  []*net.IPNet
	CORSAllowedOrigins []string
	RateLimitStore     middleware.RateLimitStore
}

func NewRouter(deps RouterDependencies) *chi.Mux {
	r := chi.NewRouter()

	r.Use(middleware.RequestID)
	r.Use(middleware.ClientIPContext(deps.TrustedProxyCIDRs))
	r.Use(middleware.Logging)
	r.Use(middleware.Recover)
	r.Use(middleware.SecurityHeaders(deps.Env))
	r.Use(middleware.CORS(deps.CORSAllowedOrigins))

	store := deps.RateLimitStore
	if store == nil {
		store = middleware.NewMemoryRateLimitStore()
	}
	// All limiters share one store, so each needs its own key scope. Without it the first limiter
	// to see a client creates the bucket and every other limiter silently reuses its parameters
	// (the OTP limit used to be swallowed by the general public limit this way).
	publicRateLimiter := middleware.NewRateLimiterWithStoreAndProxies("public", store, rate.Every(3*time.Second), 20, deps.TrustedProxyCIDRs)
	protectedRateLimiter := middleware.NewRateLimiterWithStoreAndProxies("protected", store, rate.Every(6*time.Second), 10, deps.TrustedProxyCIDRs)
	otpRateLimiter := middleware.NewRateLimiterWithStoreAndProxies("otp", store, rate.Every(30*time.Second), 3, deps.TrustedProxyCIDRs)
	// The admin console loads several panels at once and refreshes them, so it gets its own, more
	// generous per-user budget instead of the tight one that protects ordinary client traffic.
	adminRateLimiter := middleware.NewRateLimiterWithStoreAndProxies("admin", store, rate.Every(250*time.Millisecond), 40, deps.TrustedProxyCIDRs)
	presence := middleware.Presence(deps.DeviceRepository, 45*time.Second)

	r.Group(func(r chi.Router) {
		r.Use(publicRateLimiter.LimitByIP)

		r.Get("/health", func(w http.ResponseWriter, _ *http.Request) {
			response.JSON(w, http.StatusOK, map[string]string{"status": "ok"})
		})

		if deps.LegalHandler != nil {
			r.Get("/privacy-policy", deps.LegalHandler.PrivacyPolicy)
			r.Get("/terms", deps.LegalHandler.Terms)
			r.Get("/licenses", deps.LegalHandler.Licenses)
			r.Get("/account-deletion", deps.LegalHandler.AccountDeletion)
		}

		if deps.AuthHandler != nil {
			r.With(otpRateLimiter.LimitByIP).Post("/auth/otp/request", deps.AuthHandler.RequestOTP)
			r.Post("/auth/register", deps.AuthHandler.Register)
			r.Post("/auth/login/params", deps.AuthHandler.LoginParams)
			r.Post("/auth/login", deps.AuthHandler.Login)
			r.Post("/auth/refresh", deps.AuthHandler.Refresh)
			r.Post("/auth/recovery/prepare", deps.AuthHandler.PrepareRecovery)
			r.Post("/auth/password/reset", deps.AuthHandler.ResetPassword)
			r.Post("/auth/logout", deps.AuthHandler.Logout)
		}
	})

	r.Group(func(r chi.Router) {
		r.Use(middleware.AuthWithDevices(deps.JWTSecret, deps.UserRepository, deps.DeviceRepository))
		r.Use(protectedRateLimiter.LimitByUser)
		r.Use(presence)

		if deps.AuthHandler != nil {
			r.Post("/api/v1/auth/password/change", deps.AuthHandler.ChangePassword)
			r.Get("/api/v1/account/profile", deps.AuthHandler.GetProfile)
			r.Put("/api/v1/account/profile", deps.AuthHandler.UpdateProfile)
			// POST (not DELETE) because the password must travel in the body and some HTTP
			// clients, including Android's HttpURLConnection, do not send bodies with DELETE.
			r.Post("/api/v1/account/delete", deps.AuthHandler.DeleteAccount)
		}
		if deps.SyncHandler != nil {
			r.Post("/api/v1/sync", deps.SyncHandler.HandleSync)
		}
		if deps.DeviceHandler != nil {
			r.Get("/api/v1/devices", deps.DeviceHandler.List)
			r.Get("/api/v1/devices/events", deps.DeviceHandler.Events)
			r.Delete("/api/v1/devices/{deviceID}", deps.DeviceHandler.Revoke)
		}
	})

	if deps.AdminHandler != nil || deps.AdminPanelHandler != nil {
		r.Group(func(r chi.Router) {
			r.Use(middleware.AuthWithDevices(deps.JWTSecret, deps.UserRepository, deps.DeviceRepository))
			r.Use(adminRateLimiter.LimitByUser)
			r.Use(presence)
			r.Route("/api/v1/admin", func(r chi.Router) {
				r.Use(middleware.RequireRoles(entity.RoleAdmin))
				if deps.AdminGate != nil {
					r.Use(middleware.RequireActiveAdmin(deps.AdminGate))
				}
				if deps.AdminHandler != nil {
					r.Put("/users/{userID}/role", deps.AdminHandler.UpdateUserRole)
				}
				if p := deps.AdminPanelHandler; p != nil {
					r.Get("/me", p.Me)
					r.Get("/users", p.ListUsers)
					r.Get("/users/{userID}", p.GetUser)
					// PUT (not PATCH): Android's HttpURLConnection cannot send PATCH.
					r.Put("/users/{userID}", p.UpdateUser)
					r.Delete("/users/{userID}", p.DeleteUser)
					r.Post("/users/{userID}/restore", p.RestoreUser)
					r.Post("/users/{userID}/unlock", p.UnlockUser)
					r.Post("/users/{userID}/revoke-sessions", p.RevokeSessions)
					r.Get("/events", p.Events)
					r.Get("/analytics", p.Analytics)
				}
			})
		})
	}

	return r
}
