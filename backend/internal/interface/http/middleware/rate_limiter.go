package middleware

import (
	"net"
	"net/http"
	"sync"
	"time"

	"github.com/mosinaRZ/finance-sync-backend/internal/domain/apperror"
	"github.com/mosinaRZ/finance-sync-backend/internal/interface/http/contextkeys"
	"github.com/mosinaRZ/finance-sync-backend/internal/pkg/response"

	"golang.org/x/time/rate"
)

type clientLimiter struct {
	limiter  *rate.Limiter
	lastSeen time.Time
}

type RateLimiter struct {
	mu          sync.Mutex
	clients     map[string]*clientLimiter
	r           rate.Limit
	burst       int
	lastCleanup time.Time
}

// NewRateLimiter یک نمونه جدید از محدودکننده با نرخ و Burst دلخواه می‌سازد
func NewRateLimiter(r rate.Limit, burst int) *RateLimiter {
	return &RateLimiter{
		clients: make(map[string]*clientLimiter),
		r:       r,
		burst:   burst,
	}
}

func (rl *RateLimiter) getLimiter(key string) *rate.Limiter {
	rl.mu.Lock()
	defer rl.mu.Unlock()

	now := time.Now()
	if now.Sub(rl.lastCleanup) >= 3*time.Minute {
		for clientKey, client := range rl.clients {
			if now.Sub(client.lastSeen) > 3*time.Minute {
				delete(rl.clients, clientKey)
			}
		}
		rl.lastCleanup = now
	}

	c, exists := rl.clients[key]
	if !exists {
		c = &clientLimiter{
			limiter:  rate.NewLimiter(rl.r, rl.burst),
			lastSeen: now,
		}
		rl.clients[key] = c
	} else {
		c.lastSeen = now
	}

	return c.limiter
}

// LimitByIP میدلور برای محدود کردن درخواست‌ها بر اساس IP (مفید برای مسیرهای عمومی)
func (rl *RateLimiter) LimitByIP(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		ip, _, err := net.SplitHostPort(r.RemoteAddr)
		if err != nil {
			ip = r.RemoteAddr
		}

		limiter := rl.getLimiter(ip)
		if !limiter.Allow() {
			errObj := apperror.ErrRateLimited("Too many requests from this IP")
			response.Error(w, errObj)
			return
		}

		next.ServeHTTP(w, r)
	})
}

// LimitByUser میدلور برای محدود کردن درخواست‌ها بر اساس UserID استخراج‌شده (مفید برای مسیر /sync)
func (rl *RateLimiter) LimitByUser(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		userID, ok := contextkeys.UserID(r.Context())
		if !ok || userID == "" {
			errObj := apperror.ErrUnauthorized("User context not found for rate limiting")
			response.Error(w, errObj)
			return
		}

		limiter := rl.getLimiter(userID)
		if !limiter.Allow() {
			errObj := apperror.ErrRateLimited("Too many sync requests")
			response.Error(w, errObj)
			return
		}

		next.ServeHTTP(w, r)
	})
}
