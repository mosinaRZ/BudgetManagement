package http_test

import (
	"context"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"
	"time"

	"github.com/go-playground/validator/v10"

	infraauth "github.com/mosinaRZ/finance-sync-backend/internal/infrastructure/auth"
	httpiface "github.com/mosinaRZ/finance-sync-backend/internal/interface/http"
	"github.com/mosinaRZ/finance-sync-backend/internal/interface/http/handler"
	authUsecase "github.com/mosinaRZ/finance-sync-backend/internal/usecase/auth"
	"github.com/mosinaRZ/finance-sync-backend/internal/usecase/mocks"
	syncUsecase "github.com/mosinaRZ/finance-sync-backend/internal/usecase/sync"
)

func TestRouterHealthIsPublic(t *testing.T) {
	r := httpiface.NewRouter(httpiface.RouterDependencies{JWTSecret: "01234567890123456789012345678901"})
	req := httptest.NewRequest(http.MethodGet, "/health", nil)
	rec := httptest.NewRecorder()
	r.ServeHTTP(rec, req)
	if rec.Code != http.StatusOK {
		t.Fatalf("status = %d, want 200", rec.Code)
	}
}

func TestRouterProtectsSyncEndpoint(t *testing.T) {
	secret := "01234567890123456789012345678901"
	validate := validator.New()
	syncUC := syncUsecase.NewSyncUsecase(&mocks.MockSyncRepository{})
	syncHandler := handler.NewSyncHandler(syncUC, validate)
	r := httpiface.NewRouter(httpiface.RouterDependencies{SyncHandler: syncHandler, JWTSecret: secret, UserRepository: &mocks.MockUserRepository{}})

	unauthenticated := httptest.NewRequest(http.MethodPost, "/api/v1/sync", nil)
	unauthenticatedRec := httptest.NewRecorder()
	r.ServeHTTP(unauthenticatedRec, unauthenticated)
	if unauthenticatedRec.Code != http.StatusUnauthorized {
		t.Fatalf("unauthenticated status = %d, want 401", unauthenticatedRec.Code)
	}

	token, err := infraauth.GenerateAccessToken("user-1", time.Minute, secret)
	if err != nil {
		t.Fatal(err)
	}
	authenticated := httptest.NewRequest(http.MethodPost, "/api/v1/sync", nil)
	authenticated.Header.Set("Authorization", "Bearer "+token)
	authenticatedRec := httptest.NewRecorder()
	r.ServeHTTP(authenticatedRec, authenticated)
	if authenticatedRec.Code == http.StatusUnauthorized {
		t.Fatal("valid access token was rejected by router authentication")
	}
}

type fakeOTPService struct{ calls int }

func (f *fakeOTPService) Request(context.Context, authUsecase.RequestOTPInput) (authUsecase.RequestOTPOutput, error) {
	f.calls++
	return authUsecase.RequestOTPOutput{ChallengeID: "c", ExpiresAt: 1}, nil
}
func (f *fakeOTPService) Verify(context.Context, authUsecase.VerifyOTPInput) (string, error) {
	return "", nil
}

// Regression: the OTP limit (3 requests, then one per 30s per IP) was never enforced because the
// public limiter, which runs first, created the shared per-IP bucket (20 requests).
func TestRouterEnforcesOTPRateLimitPerIP(t *testing.T) {
	otp := &fakeOTPService{}
	r := httpiface.NewRouter(httpiface.RouterDependencies{
		AuthHandler: handler.NewAuthHandler(nil, validator.New(), otp),
		JWTSecret:   "01234567890123456789012345678901",
	})
	post := func(remote, path string) int {
		body := strings.NewReader(`{"destination":"+989121234567","channel":"sms","purpose":"REGISTER"}`)
		req := httptest.NewRequest(http.MethodPost, path, body)
		req.Header.Set("Content-Type", "application/json")
		req.RemoteAddr = remote
		rec := httptest.NewRecorder()
		r.ServeHTTP(rec, req)
		return rec.Code
	}
	const attacker = "203.0.113.50:5555"
	for i := 1; i <= 3; i++ {
		if got := post(attacker, "/auth/otp/request"); got != http.StatusOK {
			t.Fatalf("OTP request %d: status %d, want 200", i, got)
		}
	}
	if got := post(attacker, "/auth/otp/request"); got != http.StatusTooManyRequests {
		t.Fatalf("4th OTP request from one IP: status %d, want 429", got)
	}
	if otp.calls != 3 {
		t.Fatalf("OTP service reached %d times, want 3 (no SMS may be sent past the limit)", otp.calls)
	}
	// The strict OTP bucket must not starve other public endpoints of the same IP...
	if got := post(attacker, "/health"); got == http.StatusTooManyRequests {
		t.Fatal("/health must still be served; only the OTP bucket is exhausted")
	}
	// ...and another IP keeps its own OTP allowance.
	if got := post("203.0.113.51:5555", "/auth/otp/request"); got != http.StatusOK {
		t.Fatalf("different IP: status %d, want 200", got)
	}
}
