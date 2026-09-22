package http_test

import (
	"net/http"
	"net/http/httptest"
	"testing"
	"time"

	"github.com/go-playground/validator/v10"

	infraauth "github.com/mosinaRZ/finance-sync-backend/internal/infrastructure/auth"
	httpiface "github.com/mosinaRZ/finance-sync-backend/internal/interface/http"
	"github.com/mosinaRZ/finance-sync-backend/internal/interface/http/handler"
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
