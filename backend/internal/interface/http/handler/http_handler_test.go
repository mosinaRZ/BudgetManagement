package handler

import (
	"context"
	"encoding/json"
	"errors"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"

	"github.com/go-playground/validator/v10"

	"github.com/mosinaRZ/finance-sync-backend/internal/domain/apperror"
	"github.com/mosinaRZ/finance-sync-backend/internal/interface/http/dto"
	authusecase "github.com/mosinaRZ/finance-sync-backend/internal/usecase/auth"
)

type fakeAuthService struct {
	register func(context.Context, authusecase.RegisterInput) (authusecase.RegisterOutput, error)
	login    func(context.Context, authusecase.LoginInput) (authusecase.LoginOutput, error)
	refresh  func(context.Context, authusecase.RefreshInput) (authusecase.RefreshOutput, error)
}

func (f fakeAuthService) Register(ctx context.Context, in authusecase.RegisterInput) (authusecase.RegisterOutput, error) {
	return f.register(ctx, in)
}
func (f fakeAuthService) Login(ctx context.Context, in authusecase.LoginInput) (authusecase.LoginOutput, error) {
	return f.login(ctx, in)
}
func (f fakeAuthService) Refresh(ctx context.Context, in authusecase.RefreshInput) (authusecase.RefreshOutput, error) {
	return f.refresh(ctx, in)
}

func TestAuthHandlerRejectsMalformedJSON(t *testing.T) {
	h := NewAuthHandler(fakeAuthService{}, validator.New())
	req := httptest.NewRequest(http.MethodPost, "/auth/login", strings.NewReader("{"))
	rec := httptest.NewRecorder()
	h.Login(rec, req)
	if rec.Code != http.StatusBadRequest {
		t.Fatalf("status = %d, want %d", rec.Code, http.StatusBadRequest)
	}
}

func TestAuthHandlerMapsUsecaseError(t *testing.T) {
	h := NewAuthHandler(fakeAuthService{login: func(context.Context, authusecase.LoginInput) (authusecase.LoginOutput, error) {
		return authusecase.LoginOutput{}, apperror.ErrUnauthorized("invalid credentials")
	}}, validator.New())
	body, _ := json.Marshal(dto.LoginRequest{PhoneNumber: "+989121234567", Password: "correct horse", DeviceID: "device"})
	req := httptest.NewRequest(http.MethodPost, "/auth/login", strings.NewReader(string(body)))
	rec := httptest.NewRecorder()
	h.Login(rec, req)
	if rec.Code != http.StatusUnauthorized {
		t.Fatalf("status = %d, want %d", rec.Code, http.StatusUnauthorized)
	}
}

func TestAuthHandlerDoesNotExposeUnexpectedError(t *testing.T) {
	h := NewAuthHandler(fakeAuthService{login: func(context.Context, authusecase.LoginInput) (authusecase.LoginOutput, error) {
		return authusecase.LoginOutput{}, errors.New("mongo secret details")
	}}, validator.New())
	body := `{"phone_number":"+989121234567","password":"correct horse","device_id":"device"}`
	req := httptest.NewRequest(http.MethodPost, "/auth/login", strings.NewReader(body))
	rec := httptest.NewRecorder()
	h.Login(rec, req)
	if rec.Code != http.StatusInternalServerError {
		t.Fatalf("status = %d, want %d", rec.Code, http.StatusInternalServerError)
	}
	if strings.Contains(rec.Body.String(), "mongo secret details") {
		t.Fatal("internal error leaked to client")
	}
}
