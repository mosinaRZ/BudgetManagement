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
