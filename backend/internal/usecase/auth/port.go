package auth

import (
	"context"

	"github.com/mosinaRZ/finance-sync-backend/internal/domain/entity"
)

// DeviceInfo describes the calling device. Every value is untrusted display data
// and is sanitised by entity.Device.ApplyMetadata before it is stored. IP is the
// client address resolved by the HTTP layer (never taken from the request body).
type DeviceInfo struct {
	Name, Model, Platform, OSVersion, AppVersion, IP, Fingerprint string
}

type RegisterInput struct {
	PhoneNumber, Email, Password, DeviceID                                                        string
	OTPChallengeID, OTPCode                                                                       string
	EmailOTPChallengeID, EmailOTPCode                                                             string
	KdfSalt                                                                                       string
	PasswordKeyEnvelope, PasswordKeyNonce, RecoveryKeyHash, RecoveryKeyEnvelope, RecoveryKeyNonce []byte

	Device DeviceInfo
}
type RegisterOutput struct {
	AccessToken, RefreshToken, KdfSalt, UserID                                   string
	Role                                                                         entity.Role
	RecoveryRequired                                                             bool
	PasswordKeyEnvelope, PasswordKeyNonce, RecoveryKeyEnvelope, RecoveryKeyNonce []byte
}
type LoginInput struct {
	Identifier, Password, DeviceID string
	Device                         DeviceInfo
}
type LoginOutput struct {
	AccessToken, RefreshToken, KdfSalt, UserID                                   string
	Role                                                                         entity.Role
	PasswordKeyEnvelope, PasswordKeyNonce, RecoveryKeyEnvelope, RecoveryKeyNonce []byte
}
type RefreshInput struct{ RefreshToken string }
type RefreshOutput struct {
	AccessToken, RefreshToken string
	Role                      entity.Role
}
type RequestOTPInput struct {
	Destination, Channel string
	Purpose              entity.OTPPurpose
}
type RequestOTPOutput struct {
	ChallengeID string
	ExpiresAt   int64
}
type VerifyOTPInput struct {
	ChallengeID, Code string
	Purpose           entity.OTPPurpose
}
type PrepareRecoveryInput struct {
	ChallengeID, OTPCode, RecoveryKey string
}
type PrepareRecoveryOutput struct {
	RecoverySessionToken, KdfSalt, UserID string
	RecoveryKeyEnvelope, RecoveryKeyNonce []byte
}
type ResetPasswordInput struct {
	RecoverySessionToken, NewPassword     string
	DeviceID                              string
	KdfSalt                               string
	PasswordKeyEnvelope, PasswordKeyNonce []byte

	Device DeviceInfo
}
type ResetPasswordOutput struct {
	AccessToken, RefreshToken, KdfSalt, UserID                                   string
	Role                                                                         entity.Role
	PasswordKeyEnvelope, PasswordKeyNonce, RecoveryKeyEnvelope, RecoveryKeyNonce []byte
}

// ChangePasswordInput changes the password of an already authenticated user.
// KdfSalt must equal the account's existing salt: the recovery envelope is derived
// with the same salt, so rotating it would silently break account recovery.
type ChangePasswordInput struct {
	UserID, CurrentPassword, NewPassword, DeviceID, KdfSalt string
	PasswordKeyEnvelope, PasswordKeyNonce                   []byte

	Device DeviceInfo
}

// ChangePasswordOutput is a fresh session for the calling device.
type ChangePasswordOutput = ResetPasswordOutput

type OTPService interface {
	Request(context.Context, RequestOTPInput) (RequestOTPOutput, error)
	Verify(context.Context, VerifyOTPInput) (string, error)
}
type Service interface {
	Register(context.Context, RegisterInput) (RegisterOutput, error)
	Login(context.Context, LoginInput) (LoginOutput, error)
	Refresh(context.Context, RefreshInput) (RefreshOutput, error)
}
type ExtendedService interface {
	Service
	Logout(context.Context, string) error
	PrepareRecovery(context.Context, PrepareRecoveryInput) (PrepareRecoveryOutput, error)
	ResetPassword(context.Context, ResetPasswordInput) (ResetPasswordOutput, error)
}

// PasswordChanger is implemented by services that support changing the password of a
// signed-in user. It is a separate interface so existing Service fakes keep compiling.
type PasswordChanger interface {
	ChangePassword(context.Context, ChangePasswordInput) (ChangePasswordOutput, error)
}

type Profile struct {
	UserID        string
	PhoneNumber   string
	Email         string
	EmailVerified bool
	FirstName     string
	LastName      string
	Gender        entity.Gender
	BirthDate     string
}

type UpdateProfileInput struct {
	UserID, FirstName, LastName, BirthDate string
	Gender                                 entity.Gender
}

type UpdateEmailInput struct {
	UserID, Email, OTPChallengeID, OTPCode string
}

type ProfileService interface {
	GetProfile(context.Context, string) (Profile, error)
	UpdateProfile(context.Context, UpdateProfileInput) (Profile, error)
	UpdateEmail(context.Context, UpdateEmailInput) (Profile, error)
}
