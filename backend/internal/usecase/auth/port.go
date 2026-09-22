package auth

import (
	"context"

	"github.com/mosinaRZ/finance-sync-backend/internal/domain/entity"
)

type RegisterInput struct {
	PhoneNumber, Email, Password, DeviceID                                                        string
	OTPChallengeID, OTPCode                                                                       string
	EmailOTPChallengeID, EmailOTPCode                                                             string
	KdfSalt                                                                                       string
	PasswordKeyEnvelope, PasswordKeyNonce, RecoveryKeyHash, RecoveryKeyEnvelope, RecoveryKeyNonce []byte
}
type RegisterOutput struct {
	AccessToken, RefreshToken, KdfSalt, UserID                                   string
	Role                                                                         entity.Role
	RecoveryRequired                                                             bool
	PasswordKeyEnvelope, PasswordKeyNonce, RecoveryKeyEnvelope, RecoveryKeyNonce []byte
}
type LoginInput struct{ Identifier, Password, DeviceID string }
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
}
type ResetPasswordOutput struct {
	AccessToken, RefreshToken, KdfSalt, UserID                                   string
	Role                                                                         entity.Role
	PasswordKeyEnvelope, PasswordKeyNonce, RecoveryKeyEnvelope, RecoveryKeyNonce []byte
}
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
