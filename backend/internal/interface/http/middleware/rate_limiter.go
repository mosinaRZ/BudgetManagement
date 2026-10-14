package middleware

import (
	"context"
	"net"
	"net/http"
	"strings"
	"sync"
	"time"

	"github.com/mosinaRZ/finance-sync-backend/internal/domain/apperror"
	"github.com/mosinaRZ/finance-sync-backend/internal/interface/http/contextkeys"
	"github.com/mosinaRZ/finance-sync-backend/internal/pkg/response"
	"golang.org/x/time/rate"
)

type RateLimitStore interface {
	Allow(ctx context.Context, key string, limit rate.Limit, burst int) (bool, error)
}

const (
	// limiterIdleTTL is how long an idle key is kept before it becomes eligible for eviction.
	limiterIdleTTL = 3 * time.Minute
	// limiterSweepInterval bounds how often the map is scanned for idle keys, so the
	// per-request cost stays O(1) instead of O(number of tracked clients).
	limiterSweepInterval = time.Minute
)

// memoryRateLimitStore is an in-process token-bucket store. It is correct for a
// single API instance (the supported deployment topology): state is lost on
// restart and is not shared between processes.
type memoryRateLimitStore struct {
	mu        sync.Mutex
	clients   map[string]*clientLimiter
	lastSweep time.Time
}
type clientLimiter struct {
	limiter  *rate.Limiter
	lastSeen time.Time
}

func NewMemoryRateLimitStore() RateLimitStore {
	return &memoryRateLimitStore{clients: make(map[string]*clientLimiter), lastSweep: time.Now()}
}
func (s *memoryRateLimitStore) Allow(_ context.Context, key string, r rate.Limit, burst int) (bool, error) {
	s.mu.Lock()
	defer s.mu.Unlock()
	now := time.Now()
	if now.Sub(s.lastSweep) >= limiterSweepInterval {
		for k, c := range s.clients {
			if now.Sub(c.lastSeen) > limiterIdleTTL {
				delete(s.clients, k)
			}
		}
		s.lastSweep = now
	}
	c := s.clients[key]
	if c == nil {
		c = &clientLimiter{limiter: rate.NewLimiter(r, burst)}
		s.clients[key] = c
	}
	c.lastSeen = now
	return c.limiter.Allow(), nil
}

type RateLimiter struct {
	store          RateLimitStore
	r              rate.Limit
	burst          int
	trustedProxies []*net.IPNet
}

func NewRateLimiter(r rate.Limit, burst int) *RateLimiter {
	return &RateLimiter{store: NewMemoryRateLimitStore(), r: r, burst: burst}
}
func NewRateLimiterWithStore(store RateLimitStore, r rate.Limit, burst int) *RateLimiter {
	if store == nil {
		store = NewMemoryRateLimitStore()
	}
	return &RateLimiter{store: store, r: r, burst: burst}
}
func NewRateLimiterWithStoreAndProxies(store RateLimitStore, r rate.Limit, burst int, trusted []*net.IPNet) *RateLimiter {
	if store == nil {
		store = NewMemoryRateLimitStore()
	}
	return &RateLimiter{store: store, r: r, burst: burst, trustedProxies: trusted}
}

func (rl *RateLimiter) LimitByIP(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		ip := ClientIP(r, rl.trustedProxies)
		allowed, err := rl.store.Allow(r.Context(), "ip:"+ip, rl.r, rl.burst)
		if err != nil {
			response.Error(w, apperror.ErrInternal("rate limiter unavailable"))
			return
		}
		if !allowed {
			response.Error(w, apperror.ErrRateLimited("Too many requests from this IP"))
			return
		}
		next.ServeHTTP(w, r)
	})
}
func (rl *RateLimiter) LimitByUser(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		userID, ok := contextkeys.UserID(r.Context())
		if !ok || userID == "" {
			response.Error(w, apperror.ErrUnauthorized("User context not found for rate limiting"))
			return
		}
		allowed, err := rl.store.Allow(r.Context(), "user:"+userID, rl.r, rl.burst)
		if err != nil {
			response.Error(w, apperror.ErrInternal("rate limiter unavailable"))
			return
		}
		if !allowed {
			response.Error(w, apperror.ErrRateLimited("Too many sync requests"))
			return
		}
		next.ServeHTTP(w, r)
	})
}

// ClientIP trusts forwarding headers only when the immediate peer is in the configured proxy CIDRs.
func ClientIP(r *http.Request, trusted []*net.IPNet) string {
	remote := r.RemoteAddr
	host, _, err := net.SplitHostPort(remote)
	if err != nil {
		host = remote
	}
	peer := net.ParseIP(strings.TrimSpace(host))
	trustedPeer := false
	if peer != nil {
		for _, n := range trusted {
			if n.Contains(peer) {
				trustedPeer = true
				break
			}
		}
	}
	if trustedPeer {
		if xff := r.Header.Get("X-Forwarded-For"); xff != "" {
			if first := strings.TrimSpace(strings.Split(xff, ",")[0]); net.ParseIP(first) != nil {
				return first
			}
		}
		if real := strings.TrimSpace(r.Header.Get("X-Real-IP")); net.ParseIP(real) != nil {
			return real
		}
	}
	if peer != nil {
		return peer.String()
	}
	return host
}
