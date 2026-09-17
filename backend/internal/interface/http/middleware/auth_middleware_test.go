package middleware

import (
	"net/http"
	"net/http/httptest"
	"testing"
	"time"

	"github.com/mosinaRZ/finance-sync-backend/internal/domain/entity"
	infraauth "github.com/mosinaRZ/finance-sync-backend/internal/infrastructure/auth"
	"github.com/mosinaRZ/finance-sync-backend/internal/interface/http/contextkeys"
)

func TestAuthMiddlewareRejectsMissingToken(t *testing.T) {
	nextCalled := false
	h := Auth("01234567890123456789012345678901")(http.HandlerFunc(func(http.ResponseWriter, *http.Request) { nextCalled = true }))
	req := httptest.NewRequest(http.MethodPost, "/api/v1/sync", nil)
	rec := httptest.NewRecorder()
	h.ServeHTTP(rec, req)
	if rec.Code != http.StatusUnauthorized || nextCalled {
		t.Fatalf("status=%d nextCalled=%v", rec.Code, nextCalled)
	}
}

func TestAuthMiddlewareRejectsExpiredToken(t *testing.T) {
	secret := "01234567890123456789012345678901"
	token, err := infraauth.GenerateAccessToken("user-1", time.Nanosecond, secret)
	if err != nil {
		t.Fatal(err)
	}
	time.Sleep(2 * time.Millisecond)
	h := Auth(secret)(http.HandlerFunc(func(http.ResponseWriter, *http.Request) { t.Fatal("next should not be called") }))
	req := httptest.NewRequest(http.MethodPost, "/api/v1/sync", nil)
	req.Header.Set("Authorization", "Bearer "+token)
	rec := httptest.NewRecorder()
	h.ServeHTTP(rec, req)
	if rec.Code != http.StatusUnauthorized {
		t.Fatalf("status=%d, want 401", rec.Code)
	}
}

func TestAuthMiddlewareStoresOnlyValidatedUserIDInContext(t *testing.T) {
	secret := "01234567890123456789012345678901"
	token, err := infraauth.GenerateAccessToken("user-1", time.Minute, secret)
	if err != nil {
		t.Fatal(err)
	}
	var got string
	h := Auth(secret)(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		got, _ = contextkeys.UserID(r.Context())
		w.WriteHeader(http.StatusNoContent)
	}))
	req := httptest.NewRequest(http.MethodPost, "/api/v1/sync", nil)
	req.Header.Set("Authorization", "Bearer "+token)
	rec := httptest.NewRecorder()
	h.ServeHTTP(rec, req)
	if rec.Code != http.StatusNoContent || got != "user-1" {
		t.Fatalf("status=%d userID=%q", rec.Code, got)
	}
}

func TestRequireRolesRejectsNonAdmin(t *testing.T) {
	secret := "01234567890123456789012345678901"
	token, err := infraauth.GenerateAccessToken("user-1", time.Minute, secret)
	if err != nil {
		t.Fatal(err)
	}
	r := http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.WriteHeader(http.StatusNoContent)
	})
	req := httptest.NewRequest(http.MethodGet, "/admin", nil)
	req.Header.Set("Authorization", "Bearer "+token)
	rec := httptest.NewRecorder()
	Auth(secret)(RequireRoles(entity.RoleAdmin)(r)).ServeHTTP(rec, req)
	if rec.Code != http.StatusForbidden {
		t.Fatalf("status = %d, want 403", rec.Code)
	}
}

func TestRequireRolesAllowsAdmin(t *testing.T) {
	secret := "01234567890123456789012345678901"
	token, err := infraauth.GenerateAccessTokenWithRole("admin-1", entity.RoleAdmin, time.Minute, secret)
	if err != nil {
		t.Fatal(err)
	}
	r := http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if role, ok := contextkeys.Role(r.Context()); !ok || role != string(entity.RoleAdmin) {
			t.Fatalf("role in context = %q, %v", role, ok)
		}
		w.WriteHeader(http.StatusNoContent)
	})
	req := httptest.NewRequest(http.MethodGet, "/admin", nil)
	req.Header.Set("Authorization", "Bearer "+token)
	rec := httptest.NewRecorder()
	Auth(secret)(RequireRoles(entity.RoleAdmin)(r)).ServeHTTP(rec, req)
	if rec.Code != http.StatusNoContent {
		t.Fatalf("status = %d, want 204", rec.Code)
	}
}
