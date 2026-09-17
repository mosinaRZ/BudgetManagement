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
	"github.com/redis/go-redis/v9"
	"golang.org/x/time/rate"
)

type RateLimitStore interface {
	Allow(ctx context.Context, key string, limit rate.Limit, burst int) (bool, error)
}

type memoryRateLimitStore struct {
	mu      sync.Mutex
	clients map[string]*clientLimiter
}
type clientLimiter struct {
	limiter  *rate.Limiter
	lastSeen time.Time
}

func NewMemoryRateLimitStore() RateLimitStore {
	return &memoryRateLimitStore{clients: make(map[string]*clientLimiter)}
}
func (s *memoryRateLimitStore) Allow(_ context.Context, key string, r rate.Limit, burst int) (bool, error) {
	s.mu.Lock()
	defer s.mu.Unlock()
	now := time.Now()
	for k, c := range s.clients {
		if now.Sub(c.lastSeen) > 3*time.Minute {
			delete(s.clients, k)
		}
	}
	c := s.clients[key]
	if c == nil {
		c = &clientLimiter{limiter: rate.NewLimiter(r, burst)}
		s.clients[key] = c
	}
	c.lastSeen = now
	return c.limiter.Allow(), nil
}

type RedisRateLimitStore struct{ client *redis.Client }

func NewRedisRateLimitStore(client *redis.Client) *RedisRateLimitStore {
	return &RedisRateLimitStore{client: client}
}

const tokenBucketLua = `
local key = KEYS[1]
local limit = tonumber(ARGV[1])
local burst = tonumber(ARGV[2])
local now = tonumber(ARGV[3])
local refill = tonumber(ARGV[4])
local ttl = tonumber(ARGV[5])
local values = redis.call('HMGET', key, 'tokens', 'timestamp')
local tokens = tonumber(values[1])
local timestamp = tonumber(values[2])
if tokens == nil then tokens = burst end
if timestamp == nil then timestamp = now end
tokens = math.min(burst, tokens + math.max(0, now - timestamp) * refill)
local allowed = 0
if tokens >= 1 then tokens = tokens - 1; allowed = 1 end
redis.call('HSET', key, 'tokens', tokens, 'timestamp', now)
redis.call('EXPIRE', key, ttl)
return allowed
`

func (s *RedisRateLimitStore) Allow(ctx context.Context, key string, limit rate.Limit, burst int) (bool, error) {
	if s == nil || s.client == nil {
		return false, context.Canceled
	}
	sec, err := s.client.Time(ctx).Result()
	if err != nil {
		return false, err
	}
	now := float64(sec.UnixNano()) / 1e9
	ttl := int64((time.Duration(float64(time.Second)*float64(burst)/float64(limit)) + 3*time.Minute).Seconds())
	if ttl < 60 {
		ttl = 60
	}
	result, err := s.client.Eval(ctx, tokenBucketLua, []string{"cidna:rl:" + key}, float64(limit), burst, now, float64(limit), ttl).Int()
	return result == 1, err
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
