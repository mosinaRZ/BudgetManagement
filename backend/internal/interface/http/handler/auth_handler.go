package handler

import (
	"encoding/base64"
	"net/http"

	"github.com/go-playground/validator/v10"
	"github.com/mosinaRZ/finance-sync-backend/internal/domain/apperror"
	"github.com/mosinaRZ/finance-sync-backend/internal/domain/entity"
	"github.com/mosinaRZ/finance-sync-backend/internal/interface/http/dto"
	"github.com/mosinaRZ/finance-sync-backend/internal/pkg/response"
	"github.com/mosinaRZ/finance-sync-backend/internal/usecase/auth"
)

type AuthHandler struct {
	usecase  auth.Service
	otp      auth.OTPService
	validate *validator.Validate
}

func NewAuthHandler(u auth.Service, v *validator.Validate, otp ...auth.OTPService) *AuthHandler {
	var o auth.OTPService
	if len(otp) > 0 {
		o = otp[0]
	}
	if v == nil {
		v = validator.New()
	}
	return &AuthHandler{usecase: u, otp: o, validate: v}
}
func (h *AuthHandler) RequestOTP(w http.ResponseWriter, r *http.Request) {
	var q dto.OTPRequest
	if err := decodeJSON(w, r, &q); err != nil {
		response.Error(w, err)
		return
	}
	if err := validateRequest(h.validate, q); err != nil {
		response.Error(w, err)
		return
	}
	out, err := h.otp.Request(r.Context(), auth.RequestOTPInput{Destination: q.Destination, Channel: q.Channel, Purpose: entity.OTPPurpose(q.Purpose)})
	if err != nil {
		response.Error(w, err)
		return
	}
	response.JSON(w, http.StatusOK, dto.OTPResponse{ChallengeID: out.ChallengeID, ExpiresAt: out.ExpiresAt})
}
func (h *AuthHandler) Register(w http.ResponseWriter, r *http.Request) {
	var q dto.RegisterRequest
	if err := decodeJSON(w, r, &q); err != nil {
		response.Error(w, err)
		return
	}
	if err := validateRequest(h.validate, q); err != nil {
		response.Error(w, err)
		return
	}
	passwordKeyEnvelope, err := decodeB64(q.PasswordKeyEnvelope)
	if err != nil {
		response.Error(w, apperror.ErrValidation("password_key_envelope must be valid base64"))
		return
	}
	passwordKeyNonce, err := decodeB64(q.PasswordKeyNonce)
	if err != nil {
		response.Error(w, apperror.ErrValidation("password_key_nonce must be valid base64"))
		return
	}
	recoveryKeyEnvelope, err := decodeB64(q.RecoveryKeyEnvelope)
	if err != nil {
		response.Error(w, apperror.ErrValidation("recovery_key_envelope must be valid base64"))
		return
	}
	recoveryKeyNonce, err := decodeB64(q.RecoveryKeyNonce)
	if err != nil {
		response.Error(w, apperror.ErrValidation("recovery_key_nonce must be valid base64"))
		return
	}
	recoveryKeyHash, err := decodeRecoveryHash(q.RecoveryKeyHash)
	if err != nil {
		response.Error(w, apperror.ErrValidation("recovery_key_hash must be valid base64"))
		return
	}

	in := auth.RegisterInput{
		PhoneNumber:         q.PhoneNumber,
		Email:               q.Email,
		Password:            q.Password,
		DeviceID:            q.DeviceID,
		OTPChallengeID:      q.OTPChallengeID,
		OTPCode:             q.OTPCode,
		EmailOTPChallengeID: q.EmailOTPChallengeID,
		EmailOTPCode:        q.EmailOTPCode,
		KdfSalt:             q.KdfSalt,
		PasswordKeyEnvelope: passwordKeyEnvelope,
		PasswordKeyNonce:    passwordKeyNonce,
		RecoveryKeyHash:     recoveryKeyHash,
		RecoveryKeyEnvelope: recoveryKeyEnvelope,
		RecoveryKeyNonce:    recoveryKeyNonce,
	}
	out, err := h.usecase.Register(r.Context(), in)
	if err != nil {
		response.Error(w, err)
		return
	}
	response.JSON(w, http.StatusCreated, dto.RegisterResponse{AccessToken: out.AccessToken, RefreshToken: out.RefreshToken, KdfSalt: out.KdfSalt, UserID: out.UserID, RecoveryRequired: out.RecoveryRequired, PasswordKeyEnvelope: enc(out.PasswordKeyEnvelope), PasswordKeyNonce: enc(out.PasswordKeyNonce), RecoveryKeyEnvelope: enc(out.RecoveryKeyEnvelope), RecoveryKeyNonce: enc(out.RecoveryKeyNonce), Role: out.Role})
}
func (h *AuthHandler) Login(w http.ResponseWriter, r *http.Request) {
	var q dto.LoginRequest
	if err := decodeJSON(w, r, &q); err != nil {
		response.Error(w, err)
		return
	}
	if err := validateRequest(h.validate, q); err != nil {
		response.Error(w, err)
		return
	}
	out, err := h.usecase.Login(r.Context(), auth.LoginInput{Identifier: q.Identifier, Password: q.Password, DeviceID: q.DeviceID})
	if err != nil {
		response.Error(w, err)
		return
	}
	response.JSON(w, http.StatusOK, dto.LoginResponse{AccessToken: out.AccessToken, RefreshToken: out.RefreshToken, KdfSalt: out.KdfSalt, UserID: out.UserID, PasswordKeyEnvelope: enc(out.PasswordKeyEnvelope), PasswordKeyNonce: enc(out.PasswordKeyNonce), RecoveryKeyEnvelope: enc(out.RecoveryKeyEnvelope), RecoveryKeyNonce: enc(out.RecoveryKeyNonce), Role: out.Role})
}
func (h *AuthHandler) Refresh(w http.ResponseWriter, r *http.Request) {
	var q dto.RefreshRequest
	if err := decodeJSON(w, r, &q); err != nil {
		response.Error(w, err)
		return
	}
	if err := validateRequest(h.validate, q); err != nil {
		response.Error(w, err)
		return
	}
	out, err := h.usecase.Refresh(r.Context(), auth.RefreshInput{RefreshToken: q.RefreshToken})
	if err != nil {
		response.Error(w, err)
		return
	}
	response.JSON(w, http.StatusOK, dto.RefreshResponse{AccessToken: out.AccessToken, RefreshToken: out.RefreshToken, Role: out.Role})
}
func (h *AuthHandler) PrepareRecovery(w http.ResponseWriter, r *http.Request) {
	var q dto.PrepareRecoveryRequest
	if err := decodeJSON(w, r, &q); err != nil {
		response.Error(w, err)
		return
	}
	if err := validateRequest(h.validate, q); err != nil {
		response.Error(w, err)
		return
	}
	extended, ok := h.usecase.(auth.ExtendedService)
	if !ok {
		response.Error(w, apperror.ErrInternal("recovery service is not configured"))
		return
	}
	out, err := extended.PrepareRecovery(r.Context(), auth.PrepareRecoveryInput{ChallengeID: q.OTPChallengeID, OTPCode: q.OTPCode, RecoveryKey: q.RecoveryKey})
	if err != nil {
		response.Error(w, err)
		return
	}
	response.JSON(w, http.StatusOK, dto.PrepareRecoveryResponse{RecoverySessionToken: out.RecoverySessionToken, KdfSalt: out.KdfSalt, UserID: out.UserID, RecoveryKeyEnvelope: enc(out.RecoveryKeyEnvelope), RecoveryKeyNonce: enc(out.RecoveryKeyNonce)})
}

func (h *AuthHandler) ResetPassword(w http.ResponseWriter, r *http.Request) {
	var q dto.ResetPasswordRequest
	if err := decodeJSON(w, r, &q); err != nil {
		response.Error(w, err)
		return
	}
	if err := validateRequest(h.validate, q); err != nil {
		response.Error(w, err)
		return
	}
	extended, ok := h.usecase.(auth.ExtendedService)
	if !ok {
		response.Error(w, apperror.ErrInternal("password reset service is not configured"))
		return
	}
	passwordKeyEnvelope, err := decodeB64(q.PasswordKeyEnvelope)
	if err != nil {
		response.Error(w, apperror.ErrValidation("password_key_envelope must be valid base64"))
		return
	}
	passwordKeyNonce, err := decodeB64(q.PasswordKeyNonce)
	if err != nil {
		response.Error(w, apperror.ErrValidation("password_key_nonce must be valid base64"))
		return
	}
	out, err := extended.ResetPassword(r.Context(), auth.ResetPasswordInput{RecoverySessionToken: q.RecoverySessionToken, NewPassword: q.NewPassword, DeviceID: q.DeviceID, KdfSalt: q.KdfSalt, PasswordKeyEnvelope: passwordKeyEnvelope, PasswordKeyNonce: passwordKeyNonce})
	if err != nil {
		response.Error(w, err)
		return
	}
	response.JSON(w, http.StatusOK, dto.ResetPasswordResponse{AccessToken: out.AccessToken, RefreshToken: out.RefreshToken, KdfSalt: out.KdfSalt, UserID: out.UserID, PasswordKeyEnvelope: enc(out.PasswordKeyEnvelope), PasswordKeyNonce: enc(out.PasswordKeyNonce), RecoveryKeyEnvelope: enc(out.RecoveryKeyEnvelope), RecoveryKeyNonce: enc(out.RecoveryKeyNonce), Role: out.Role})
}
func enc(b []byte) string {
	if len(b) == 0 {
		return ""
	}
	return base64.StdEncoding.EncodeToString(b)
}
func decodeB64(s string) ([]byte, error) {
	if s == "" {
		return nil, nil
	}
	b, err := base64.StdEncoding.DecodeString(s)
	if err != nil {
		return nil, err
	}
	return b, nil
}

func decodeRecoveryHash(s string) ([]byte, error) {
	if s == "" {
		return nil, nil
	}
	if b, err := base64.StdEncoding.DecodeString(s); err == nil {
		return b, nil
	}
	return base64.RawURLEncoding.DecodeString(s)
}

func (h *AuthHandler) Logout(w http.ResponseWriter, r *http.Request) {
	var q dto.LogoutRequest
	if err := decodeJSON(w, r, &q); err != nil {
		response.Error(w, err)
		return
	}
	if err := validateRequest(h.validate, q); err != nil {
		response.Error(w, err)
		return
	}
	extended, ok := h.usecase.(auth.ExtendedService)
	if !ok {
		response.Error(w, apperror.ErrInternal("logout service is not configured"))
		return
	}
	if err := extended.Logout(r.Context(), q.RefreshToken); err != nil {
		response.Error(w, err)
		return
	}
	w.WriteHeader(http.StatusNoContent)
}
