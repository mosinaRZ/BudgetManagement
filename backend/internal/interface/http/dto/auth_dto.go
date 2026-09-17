package dto

import "github.com/mosinaRZ/finance-sync-backend/internal/domain/entity"

type OTPRequest struct {
	Destination string `json:"destination" validate:"required,max=320"`
	Channel     string `json:"channel" validate:"required,oneof=sms email"`
	Purpose     string `json:"purpose" validate:"required,oneof=REGISTER LOGIN PASSWORD_RESET EMAIL_VERIFICATION"`
}
type OTPResponse struct {
	ChallengeID string `json:"challenge_id"`
	ExpiresAt   int64  `json:"expires_at"`
}
type RegisterRequest struct {
	PhoneNumber         string `json:"phone_number" validate:"required"`
	Email               string `json:"email,omitempty" validate:"omitempty,email,max=320"`
	Password            string `json:"password" validate:"required,min=8,max=256"`
	DeviceID            string `json:"device_id" validate:"required,max=128"`
	OTPChallengeID      string `json:"otp_challenge_id" validate:"required"`
	OTPCode             string `json:"otp_code" validate:"required,len=6,numeric"`
	EmailOTPChallengeID string `json:"email_otp_challenge_id,omitempty"`
	EmailOTPCode        string `json:"email_otp_code,omitempty" validate:"omitempty,len=6,numeric"`
	KdfSalt             string `json:"kdf_salt,omitempty" validate:"max=256"`
	PasswordKeyEnvelope string `json:"password_key_envelope,omitempty"`
	PasswordKeyNonce    string `json:"password_key_nonce,omitempty"`
	RecoveryKeyHash     string `json:"recovery_key_hash,omitempty"`
	RecoveryKeyEnvelope string `json:"recovery_key_envelope,omitempty"`
	RecoveryKeyNonce    string `json:"recovery_key_nonce,omitempty"`
}
type RegisterResponse struct {
	AccessToken      string      `json:"access_token"`
	RefreshToken     string      `json:"refresh_token"`
	KdfSalt          string      `json:"kdf_salt"`
	UserID           string      `json:"user_id"`
	RecoveryRequired bool        `json:"recovery_required"`
	Role             entity.Role `json:"role"`
}
type LoginRequest struct {
	Identifier string `json:"identifier" validate:"required,max=320"`
	Password   string `json:"password" validate:"required,max=256"`
	DeviceID   string `json:"device_id" validate:"required,max=128"`
}
type LoginResponse struct {
	AccessToken         string      `json:"access_token"`
	RefreshToken        string      `json:"refresh_token"`
	KdfSalt             string      `json:"kdf_salt"`
	UserID              string      `json:"user_id"`
	Role                entity.Role `json:"role"`
	PasswordKeyEnvelope string      `json:"password_key_envelope,omitempty"`
	PasswordKeyNonce    string      `json:"password_key_nonce,omitempty"`
	RecoveryKeyEnvelope string      `json:"recovery_key_envelope,omitempty"`
	RecoveryKeyNonce    string      `json:"recovery_key_nonce,omitempty"`
}
type RefreshRequest struct {
	RefreshToken string `json:"refresh_token" validate:"required"`
}
type RefreshResponse struct {
	AccessToken  string      `json:"access_token"`
	RefreshToken string      `json:"refresh_token"`
	Role         entity.Role `json:"role"`
}
type ResetPasswordRequest struct {
	OTPChallengeID      string `json:"otp_challenge_id" validate:"required"`
	OTPCode             string `json:"otp_code" validate:"required,len=6,numeric"`
	NewPassword         string `json:"new_password" validate:"required,min=8,max=256"`
	RecoveryKey         string `json:"recovery_key" validate:"required,min=16,max=512"`
	DeviceID            string `json:"device_id" validate:"required,max=128"`
	PasswordKeyEnvelope string `json:"password_key_envelope" validate:"required"`
	PasswordKeyNonce    string `json:"password_key_nonce" validate:"required"`
}
type ResetPasswordResponse struct {
	AccessToken         string      `json:"access_token"`
	RefreshToken        string      `json:"refresh_token"`
	KdfSalt             string      `json:"kdf_salt"`
	UserID              string      `json:"user_id"`
	Role                entity.Role `json:"role"`
	PasswordKeyEnvelope string      `json:"password_key_envelope"`
	PasswordKeyNonce    string      `json:"password_key_nonce"`
	RecoveryKeyEnvelope string      `json:"recovery_key_envelope"`
	RecoveryKeyNonce    string      `json:"recovery_key_nonce"`
}

type LogoutRequest struct {
	RefreshToken string `json:"refresh_token" validate:"required"`
}

type UpdateUserRoleRequest struct {
	Role entity.Role `json:"role" validate:"required"`
}

type UpdateUserRoleResponse struct {
	UserID string      `json:"user_id"`
	Role   entity.Role `json:"role"`
}
