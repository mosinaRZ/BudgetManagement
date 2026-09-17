package middleware

import (
	"net"
	"net/http"
	"net/http/httptest"
	"testing"
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
