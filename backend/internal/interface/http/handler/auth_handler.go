package handler

import (
	"net/http"

	"github.com/go-playground/validator/v10"

	"github.com/mosinaRZ/finance-sync-backend/internal/domain/apperror"
	"github.com/mosinaRZ/finance-sync-backend/internal/interface/http/dto"
	"github.com/mosinaRZ/finance-sync-backend/internal/pkg/response"
	authusecase "github.com/mosinaRZ/finance-sync-backend/internal/usecase/auth"
)

type AuthHandler struct {
	useCase  authusecase.Service
	validate *validator.Validate
}

func NewAuthHandler(uc authusecase.Service, validate *validator.Validate) *AuthHandler {
	if validate == nil {
		validate = validator.New()
	}
	return &AuthHandler{useCase: uc, validate: validate}
}

func (h *AuthHandler) Register(w http.ResponseWriter, r *http.Request) {
	if h.useCase == nil {
		response.Error(w, apperror.ErrInternal("authentication service is not configured"))
		return
	}
	var req dto.RegisterRequest
	if err := decodeJSON(w, r, &req); err != nil {
		response.Error(w, err)
		return
	}
	if err := validateRequest(h.validate, req); err != nil {
		response.Error(w, err)
		return
	}
	res, err := h.useCase.Register(r.Context(), authusecase.RegisterInput{PhoneNumber: req.PhoneNumber, Password: req.Password, DeviceID: req.DeviceID})
	if err != nil {
		response.Error(w, err)
		return
	}
	response.JSON(w, http.StatusCreated, dto.RegisterResponse{AccessToken: res.AccessToken, RefreshToken: res.RefreshToken, KdfSalt: res.KdfSalt})
}

func (h *AuthHandler) Login(w http.ResponseWriter, r *http.Request) {
	if h.useCase == nil {
		response.Error(w, apperror.ErrInternal("authentication service is not configured"))
		return
	}
	var req dto.LoginRequest
	if err := decodeJSON(w, r, &req); err != nil {
		response.Error(w, err)
		return
	}
	if err := validateRequest(h.validate, req); err != nil {
		response.Error(w, err)
		return
	}
	res, err := h.useCase.Login(r.Context(), authusecase.LoginInput{PhoneNumber: req.PhoneNumber, Password: req.Password, DeviceID: req.DeviceID})
	if err != nil {
		response.Error(w, err)
		return
	}
	response.JSON(w, http.StatusOK, dto.LoginResponse{AccessToken: res.AccessToken, RefreshToken: res.RefreshToken, KdfSalt: res.KdfSalt})
}

func (h *AuthHandler) Refresh(w http.ResponseWriter, r *http.Request) {
	if h.useCase == nil {
		response.Error(w, apperror.ErrInternal("authentication service is not configured"))
		return
	}
	var req dto.RefreshRequest
	if err := decodeJSON(w, r, &req); err != nil {
		response.Error(w, err)
		return
	}
	if err := validateRequest(h.validate, req); err != nil {
		response.Error(w, err)
		return
	}
	res, err := h.useCase.Refresh(r.Context(), authusecase.RefreshInput{RefreshToken: req.RefreshToken})
	if err != nil {
		response.Error(w, err)
		return
	}
	response.JSON(w, http.StatusOK, dto.RefreshResponse{AccessToken: res.AccessToken, RefreshToken: res.RefreshToken})
}
