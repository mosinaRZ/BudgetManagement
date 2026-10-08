package dto

import "github.com/mosinaRZ/finance-sync-backend/internal/domain/entity"

// DeviceInfoRequest is the optional, display-only description of the calling device.
// Values are untrusted: they are sanitised and length-capped by the domain layer.
type DeviceInfoRequest struct {
	Name       string `json:"name,omitempty" validate:"max=128"`
	Model      string `json:"model,omitempty" validate:"max=128"`
	Platform   string `json:"platform,omitempty" validate:"max=32"`
	OSVersion  string `json:"os_version,omitempty" validate:"max=128"`
	AppVersion string `json:"app_version,omitempty" validate:"max=64"`
	// Fingerprint is a client-side hash of the installation (hex). See entity.Device.Fingerprint.
	Fingerprint string `json:"fingerprint,omitempty" validate:"max=128"`
}

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
	PhoneNumber         string             `json:"phone_number" validate:"required"`
	Email               string             `json:"email,omitempty" validate:"omitempty,email,max=320"`
	Password            string             `json:"password" validate:"required,min=8,max=256"`
	DeviceID            string             `json:"device_id" validate:"required,max=128"`
	OTPChallengeID      string             `json:"otp_challenge_id" validate:"required"`
	OTPCode             string             `json:"otp_code" validate:"required,len=6,numeric"`
	EmailOTPChallengeID string             `json:"email_otp_challenge_id,omitempty"`
	EmailOTPCode        string             `json:"email_otp_code,omitempty" validate:"omitempty,len=6,numeric"`
	KdfSalt             string             `json:"kdf_salt,omitempty" validate:"max=256"`
	PasswordKeyEnvelope string             `json:"password_key_envelope,omitempty"`
	PasswordKeyNonce    string             `json:"password_key_nonce,omitempty"`
	RecoveryKeyHash     string             `json:"recovery_key_hash,omitempty"`
	RecoveryKeyEnvelope string             `json:"recovery_key_envelope,omitempty"`
	RecoveryKeyNonce    string             `json:"recovery_key_nonce,omitempty"`
	DeviceInfo          *DeviceInfoRequest `json:"device_info,omitempty"`
}
type RegisterResponse struct {
	AccessToken         string      `json:"access_token"`
	RefreshToken        string      `json:"refresh_token"`
	KdfSalt             string      `json:"kdf_salt"`
	UserID              string      `json:"user_id"`
	RecoveryRequired    bool        `json:"recovery_required"`
	PasswordKeyEnvelope string      `json:"password_key_envelope,omitempty"`
	PasswordKeyNonce    string      `json:"password_key_nonce,omitempty"`
	RecoveryKeyEnvelope string      `json:"recovery_key_envelope,omitempty"`
	RecoveryKeyNonce    string      `json:"recovery_key_nonce,omitempty"`
	Role                entity.Role `json:"role"`
}
type LoginRequest struct {
	Identifier string             `json:"identifier" validate:"required,max=320"`
	Password   string             `json:"password" validate:"required,max=256"`
	DeviceID   string             `json:"device_id" validate:"required,max=128"`
	DeviceInfo *DeviceInfoRequest `json:"device_info,omitempty"`
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
type PrepareRecoveryRequest struct {
	OTPChallengeID string `json:"otp_challenge_id" validate:"required"`
	OTPCode        string `json:"otp_code" validate:"required,len=6,numeric"`
	RecoveryKey    string `json:"recovery_key" validate:"required,min=16,max=512"`
}

type PrepareRecoveryResponse struct {
	RecoverySessionToken string `json:"recovery_session_token"`
	KdfSalt              string `json:"kdf_salt"`
	UserID               string `json:"user_id"`
	RecoveryKeyEnvelope  string `json:"recovery_key_envelope"`
	RecoveryKeyNonce     string `json:"recovery_key_nonce"`
}

type ResetPasswordRequest struct {
	RecoverySessionToken string             `json:"recovery_session_token" validate:"required"`
	NewPassword          string             `json:"new_password" validate:"required,min=8,max=256"`
	DeviceID             string             `json:"device_id" validate:"required,max=128"`
	KdfSalt              string             `json:"kdf_salt" validate:"required,max=256"`
	PasswordKeyEnvelope  string             `json:"password_key_envelope" validate:"required"`
	PasswordKeyNonce     string             `json:"password_key_nonce" validate:"required"`
	DeviceInfo           *DeviceInfoRequest `json:"device_info,omitempty"`
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

// ChangePasswordRequest changes the password of the authenticated account.
// The client re-wraps its data key under the new password locally (the server only
// stores the opaque envelope) and must keep using the account's existing KDF salt.
type ChangePasswordRequest struct {
	CurrentPassword     string             `json:"current_password" validate:"required,max=256"`
	NewPassword         string             `json:"new_password" validate:"required,min=8,max=256"`
	DeviceID            string             `json:"device_id" validate:"required,max=128"`
	KdfSalt             string             `json:"kdf_salt" validate:"required,max=256"`
	PasswordKeyEnvelope string             `json:"password_key_envelope" validate:"required"`
	PasswordKeyNonce    string             `json:"password_key_nonce" validate:"required"`
	DeviceInfo          *DeviceInfoRequest `json:"device_info,omitempty"`
}

// ChangePasswordResponse carries a fresh session for the calling device; every other
// session of the account is revoked by the server.
type ChangePasswordResponse struct {
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

type ProfileResponse struct {
	UserID        string        `json:"user_id"`
	PhoneNumber   string        `json:"phone_number"`
	Email         string        `json:"email,omitempty"`
	EmailVerified bool          `json:"email_verified"`
	FirstName     string        `json:"first_name,omitempty"`
	LastName      string        `json:"last_name,omitempty"`
	Gender        entity.Gender `json:"gender"`
	BirthDate     string        `json:"birth_date,omitempty"`
}

type UpdateProfileRequest struct {
	FirstName string        `json:"first_name" validate:"max=80"`
	LastName  string        `json:"last_name" validate:"max=80"`
	Gender    entity.Gender `json:"gender" validate:"required"`
	BirthDate string        `json:"birth_date" validate:"omitempty,len=10"`
}

type UpdateEmailRequest struct {
	Email          string `json:"email" validate:"required,email,max=320"`
	OTPChallengeID string `json:"otp_challenge_id" validate:"required"`
	OTPCode        string `json:"otp_code" validate:"required,len=6,numeric"`
}
