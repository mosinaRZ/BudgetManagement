package middleware

import (
	"context"
	"net"
	"net/http"
	"net/http/httptest"
	"testing"
	"time"

	"golang.org/x/time/rate"
)

func TestClientIPWithoutTrustedProxyIgnoresForwardedHeaders(t *testing.T) {
	r := httptest.NewRequest(http.MethodGet, "/", nil)
	r.RemoteAddr = "203.0.113.10:1234"
	r.Header.Set("X-Forwarded-For", "198.51.100.20")
	if got := ClientIP(r, nil); got != "203.0.113.10" {
		t.Fatalf("got %q", got)
	}
}

func TestClientIPTrustedProxyUsesLeftmostForwardedIP(t *testing.T) {
	_, cidr, _ := net.ParseCIDR("10.0.0.0/8")
	r := httptest.NewRequest(http.MethodGet, "/", nil)
	r.RemoteAddr = "10.1.2.3:443"
	r.Header.Set("X-Forwarded-For", "198.51.100.20, 10.1.2.3")
	if got := ClientIP(r, []*net.IPNet{cidr}); got != "198.51.100.20" {
		t.Fatalf("got %q", got)
	}
}

func TestMemoryRateLimitStoreEnforcesBurstPerKey(t *testing.T) {
	store := NewMemoryRateLimitStore()
	ctx := context.Background()
	// Negligible refill rate so the test does not depend on wall-clock timing.
	const slow = rate.Limit(0.0001)
	for i := 0; i < 2; i++ {
		ok, err := store.Allow(ctx, "ip:a", slow, 2)
		if err != nil || !ok {
			t.Fatalf("request %d: allowed=%v err=%v, want allowed", i+1, ok, err)
		}
	}
	if ok, _ := store.Allow(ctx, "ip:a", slow, 2); ok {
		t.Fatal("third request within burst window was allowed")
	}
	if ok, _ := store.Allow(ctx, "ip:b", slow, 2); !ok {
		t.Fatal("a different key must have its own bucket")
	}
}

func TestMemoryRateLimitStoreSweepsIdleKeys(t *testing.T) {
	s := NewMemoryRateLimitStore().(*memoryRateLimitStore)
	ctx := context.Background()
	if _, err := s.Allow(ctx, "ip:idle", rate.Every(time.Second), 1); err != nil {
		t.Fatal(err)
	}
	// Age the entry and force the next call to sweep.
	s.mu.Lock()
	s.clients["ip:idle"].lastSeen = time.Now().Add(-2 * limiterIdleTTL)
	s.lastSweep = time.Now().Add(-2 * limiterSweepInterval)
	s.mu.Unlock()

	if _, err := s.Allow(ctx, "ip:fresh", rate.Every(time.Second), 1); err != nil {
		t.Fatal(err)
	}
	s.mu.Lock()
	defer s.mu.Unlock()
	if _, ok := s.clients["ip:idle"]; ok {
		t.Fatal("idle key was not evicted")
	}
	if _, ok := s.clients["ip:fresh"]; !ok {
		t.Fatal("fresh key missing")
	}
}

func okHandler() http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, _ *http.Request) { w.WriteHeader(http.StatusOK) })
}

func statusFor(h http.Handler, remote string) int {
	req := httptest.NewRequest(http.MethodPost, "/", nil)
	req.RemoteAddr = remote
	rec := httptest.NewRecorder()
	h.ServeHTTP(rec, req)
	return rec.Code
}

// Regression: limiters sharing one store must not share buckets. Before scoping, the stricter
// limiter's parameters were ignored because the looser one had already created the bucket.
func TestRateLimitersSharingAStoreKeepIndependentBuckets(t *testing.T) {
	store := NewMemoryRateLimitStore()
	const slow = rate.Limit(0.0001)
	loose := NewRateLimiterWithStore("loose", store, slow, 20)
	strict := NewRateLimiterWithStore("strict", store, slow, 3)

	// The loose limiter always runs first, exactly like the public limiter in the router.
	h := loose.LimitByIP(strict.LimitByIP(okHandler()))
	const peer = "203.0.113.10:4000"
	for i := 1; i <= 3; i++ {
		if got := statusFor(h, peer); got != http.StatusOK {
			t.Fatalf("request %d: status %d, want 200", i, got)
		}
	}
	if got := statusFor(h, peer); got != http.StatusTooManyRequests {
		t.Fatalf("4th request: status %d, want 429 from the strict limiter", got)
	}
	// Only the strict bucket is exhausted; the loose one still has tokens for other routes.
	if got := statusFor(loose.LimitByIP(okHandler()), peer); got != http.StatusOK {
		t.Fatalf("loose limiter alone: status %d, want 200", got)
	}
	// Another client is unaffected.
	if got := statusFor(h, "203.0.113.11:4000"); got != http.StatusOK {
		t.Fatalf("other client: status %d, want 200", got)
	}
}

func TestRateLimiterKeysAreScoped(t *testing.T) {
	rec := &recordingStore{}
	NewRateLimiterWithStore("otp", rec, 1, 1).LimitByIP(okHandler()).ServeHTTP(
		httptest.NewRecorder(), func() *http.Request {
			r := httptest.NewRequest(http.MethodPost, "/", nil)
			r.RemoteAddr = "198.51.100.7:1"
			return r
		}())
	if len(rec.keys) != 1 || rec.keys[0] != "otp:ip:198.51.100.7" {
		t.Fatalf("keys = %v, want [otp:ip:198.51.100.7]", rec.keys)
	}
}

func TestRateLimiterRequiresScope(t *testing.T) {
	for _, scope := range []string{"", "  ", "a:b"} {
		func() {
			defer func() {
				if recover() == nil {
					t.Fatalf("scope %q must be rejected", scope)
				}
			}()
			NewRateLimiterWithStore(scope, nil, 1, 1)
		}()
	}
}

type recordingStore struct{ keys []string }

func (s *recordingStore) Allow(_ context.Context, key string, _ rate.Limit, _ int) (bool, error) {
	s.keys = append(s.keys, key)
	return true, nil
}
