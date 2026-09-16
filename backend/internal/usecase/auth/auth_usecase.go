package auth

import (
	"context"
	"crypto/hmac"
	"crypto/sha256"
	"encoding/base64"
	"errors"
	"github.com/mosinaRZ/finance-sync-backend/internal/domain/apperror"
	"github.com/mosinaRZ/finance-sync-backend/internal/domain/entity"
	"github.com/mosinaRZ/finance-sync-backend/internal/domain/repository"
	infraauth "github.com/mosinaRZ/finance-sync-backend/internal/infrastructure/auth"
	"strings"
	"time"
)

type ServiceImpl struct {
	users                 repository.UserRepository
	refresh               repository.RefreshTokenRepository
	devices               repository.DeviceRepository
	otp                   OTPService
	secret                string
	accessTTL, refreshTTL time.Duration
}

func NewService(
	users repository.UserRepository,
	refresh repository.RefreshTokenRepository,
	secret string,
	accessTTL time.Duration,
	refreshTTL time.Duration,
	otp ...OTPService,
) *ServiceImpl {
	var otpService OTPService
	if len(otp) > 0 {
		otpService = otp[0]
	}

	return &ServiceImpl{
		users:      users,
		refresh:    refresh,
		otp:        otpService,
		secret:     secret,
		accessTTL:  accessTTL,
		refreshTTL: refreshTTL,
	}
}
func (s *ServiceImpl) SetDeviceRepository(d repository.DeviceRepository) { s.devices = d }
func (s *ServiceImpl) Register(ctx context.Context, in RegisterInput) (RegisterOutput, error) {
	phone, err := normalizePhone(in.PhoneNumber)
	if err != nil {
		return RegisterOutput{}, apperror.ErrValidation("invalid phone number")
	}
	if len(in.Password) < 8 || len(in.Password) > 256 {
		return RegisterOutput{}, apperror.ErrValidation("password must be between 8 and 256 characters")
	}
	if err := validDevice(in.DeviceID); err != nil {
		return RegisterOutput{}, err
	}
	ph := hashIdentifier(phone, s.secret)
	if s.otp == nil {
		return RegisterOutput{}, apperror.ErrInternal("OTP service is not configured")
	}
	dest, err := s.otp.Verify(ctx, VerifyOTPInput{ChallengeID: in.OTPChallengeID, Code: in.OTPCode, Purpose: entity.OTPPurposeRegister})
	if err != nil {
		return RegisterOutput{}, err
	}
	if !hmac.Equal([]byte(dest), []byte(ph)) {
		return RegisterOutput{}, apperror.ErrUnauthorized("OTP destination does not match account identifier")
	}
	if _, err := s.users.FindByPhoneHash(ctx, ph); err == nil {
		return RegisterOutput{}, apperror.ErrConflict("user already exists")
	}
	emailHash := ""
	emailVerified := false
	if strings.TrimSpace(in.Email) != "" {
		emailHash = hashIdentifier(strings.ToLower(strings.TrimSpace(in.Email)), s.secret)
		if _, err := s.users.FindByEmailHash(ctx, emailHash); err == nil {
			return RegisterOutput{}, apperror.ErrConflict("email already exists")
		}
		if in.EmailOTPChallengeID == "" || in.EmailOTPCode == "" {
			return RegisterOutput{}, apperror.ErrValidation("email OTP is required when email is provided")
		}
		ed, err := s.otp.Verify(ctx, VerifyOTPInput{ChallengeID: in.EmailOTPChallengeID, Code: in.EmailOTPCode, Purpose: entity.OTPPurposeEmailVerification})
		if err != nil {
			return RegisterOutput{}, err
		}
		if !hmac.Equal([]byte(ed), []byte(emailHash)) {
			return RegisterOutput{}, apperror.ErrUnauthorized("email OTP destination does not match")
		}
		emailVerified = true
	}
	salt, _ := infraauth.GenerateSalt()
	passwordHash, _ := infraauth.HashPassword(in.Password, salt)
	kdfSalt := in.KdfSalt
	if kdfSalt == "" {
		b, _ := infraauth.GenerateSalt()
		kdfSalt = base64.RawStdEncoding.EncodeToString(b)
	}
	now := time.Now().UTC()
	u := &entity.User{PhoneHash: ph, EmailHash: emailHash, PhoneVerified: true, EmailVerified: emailVerified, PasswordHash: passwordHash, AuthSalt: base64.RawStdEncoding.EncodeToString(salt), KdfSalt: kdfSalt, PasswordKeyEnvelope: in.PasswordKeyEnvelope, PasswordKeyNonce: in.PasswordKeyNonce, RecoveryKeyHash: string(in.RecoveryKeyHash), RecoveryKeyEnvelope: in.RecoveryKeyEnvelope, RecoveryKeyNonce: in.RecoveryKeyNonce, CreatedAt: now, UpdatedAt: now, Devices: []string{in.DeviceID}}
	if err := s.users.Create(ctx, u); err != nil {
		return RegisterOutput{}, err
	}
	if s.devices != nil {
		now := time.Now().UTC()
		if err := s.devices.Register(ctx, &entity.Device{ID: in.DeviceID, UserID: u.ID, CreatedAt: now, LastSeenAt: now}); err != nil {
			return RegisterOutput{}, err
		}
	}
	return s.issueSession(ctx, u, in.DeviceID)
}
func (s *ServiceImpl) Login(ctx context.Context, in LoginInput) (LoginOutput, error) {
	if len(in.Password) > 256 {
		return LoginOutput{}, apperror.ErrUnauthorized("invalid credentials")
	}
	id := strings.TrimSpace(in.Identifier)
	var u *entity.User
	var err error
	if strings.Contains(id, "@") {
		u, err = s.users.FindByEmailHash(ctx, hashIdentifier(strings.ToLower(id), s.secret))
	} else {
		p, e := normalizePhone(id)
		if e != nil {
			return LoginOutput{}, apperror.ErrUnauthorized("invalid credentials")
		}
		u, err = s.users.FindByPhoneHash(ctx, hashIdentifier(p, s.secret))
	}
	if err != nil {
		return LoginOutput{}, apperror.ErrUnauthorized("invalid credentials")
	}
	ok, _ := infraauth.VerifyPassword(in.Password, u.AuthSalt, u.PasswordHash)
	if !ok {
		return LoginOutput{}, apperror.ErrUnauthorized("invalid credentials")
	}
	if err := validDevice(in.DeviceID); err != nil {
		return LoginOutput{}, err
	}
	if err := s.users.AddDevice(ctx, u.ID, in.DeviceID); err != nil {
		return LoginOutput{}, err
	}
	if s.devices != nil {
		now := time.Now().UTC()
		if err := s.devices.Register(ctx, &entity.Device{ID: in.DeviceID, UserID: u.ID, CreatedAt: now, LastSeenAt: now}); err != nil {
			return LoginOutput{}, err
		}
	}
	sess, err := s.issueRefreshAndAccess(ctx, u, in.DeviceID)
	if err != nil {
		return LoginOutput{}, err
	}
	return LoginOutput{AccessToken: sess.AccessToken, RefreshToken: sess.RefreshToken, KdfSalt: u.KdfSalt, UserID: u.ID, PasswordKeyEnvelope: u.PasswordKeyEnvelope, PasswordKeyNonce: u.PasswordKeyNonce, RecoveryKeyEnvelope: u.RecoveryKeyEnvelope, RecoveryKeyNonce: u.RecoveryKeyNonce}, nil
}
func (s *ServiceImpl) Refresh(ctx context.Context, in RefreshInput) (RefreshOutput, error) {
	if strings.TrimSpace(in.RefreshToken) == "" {
		return RefreshOutput{}, apperror.ErrUnauthorized("invalid refresh token")
	}
	h := infraauth.HashRefreshToken(in.RefreshToken)
	old, err := s.refresh.FindByHash(ctx, h)
	if err != nil {
		return RefreshOutput{}, apperror.ErrUnauthorized("invalid refresh token")
	}
	if old.RevokedAt != nil {
		_ = s.refresh.RevokeAllForUser(ctx, old.UserID)
		return RefreshOutput{}, apperror.ErrUnauthorized("refresh token reuse detected")
	}
	if !time.Now().Before(old.ExpiresAt) {
		return RefreshOutput{}, apperror.ErrUnauthorized("refresh token expired")
	}
	u, err := s.users.FindByID(ctx, old.UserID)
	if err != nil {
		return RefreshOutput{}, apperror.ErrUnauthorized("invalid refresh token")
	}
	if err := s.refresh.Revoke(ctx, h); err != nil {
		// A concurrent refresh may have revoked the same token after our read.
		// Treat that as refresh-token reuse and invalidate every session.
		if code, ok := apperror.CodeOf(err); ok && code == apperror.CodeConflict {
			_ = s.refresh.RevokeAllForUser(ctx, old.UserID)
			return RefreshOutput{}, apperror.ErrUnauthorized("refresh token reuse detected")
		}
		return RefreshOutput{}, err
	}
	sess, err := s.issueRefreshAndAccess(ctx, u, old.DeviceID)
	if err != nil {
		return RefreshOutput{}, err
	}
	return RefreshOutput{AccessToken: sess.AccessToken, RefreshToken: sess.RefreshToken}, nil
}
func (s *ServiceImpl) Logout(ctx context.Context, raw string) error {
	if strings.TrimSpace(raw) == "" {
		return apperror.ErrUnauthorized("invalid refresh token")
	}
	return s.refresh.Revoke(ctx, infraauth.HashRefreshToken(raw))
}
func (s *ServiceImpl) ResetPassword(ctx context.Context, in ResetPasswordInput) (ResetPasswordOutput, error) {
	if len(in.NewPassword) < 8 || len(in.NewPassword) > 256 {
		return ResetPasswordOutput{}, apperror.ErrValidation("password must be between 8 and 256 characters")
	}
	if err := validDevice(in.DeviceID); err != nil {
		return ResetPasswordOutput{}, err
	}
	dest, err := s.otp.Verify(ctx, VerifyOTPInput{ChallengeID: in.ChallengeID, Code: in.OTPCode, Purpose: entity.OTPPurposePasswordReset})
	if err != nil {
		return ResetPasswordOutput{}, err
	}
	u, err := s.findByDestinationHash(ctx, dest)
	if err != nil {
		return ResetPasswordOutput{}, apperror.ErrUnauthorized("invalid recovery request")
	}
	if u.RecoveryKeyHash == "" || !hmac.Equal([]byte(u.RecoveryKeyHash), []byte(hashRecovery(in.RecoveryKey))) {
		return ResetPasswordOutput{}, apperror.ErrUnauthorized("recovery key is invalid")
	}
	salt, _ := infraauth.GenerateSalt()
	ph, _ := infraauth.HashPassword(in.NewPassword, salt)
	u.PasswordHash = ph
	u.AuthSalt = base64.RawStdEncoding.EncodeToString(salt)
	u.PasswordKeyEnvelope = in.PasswordKeyEnvelope
	u.PasswordKeyNonce = in.PasswordKeyNonce
	if err := s.users.Update(ctx, u); err != nil {
		return ResetPasswordOutput{}, err
	}
	_ = s.refresh.RevokeAllForUser(ctx, u.ID)
	if s.devices != nil {
		now := time.Now().UTC()
		if err := s.devices.Register(ctx, &entity.Device{ID: in.DeviceID, UserID: u.ID, CreatedAt: now, LastSeenAt: now}); err != nil {
			return ResetPasswordOutput{}, err
		}
	}
	sess, err := s.issueRefreshAndAccess(ctx, u, in.DeviceID)
	if err != nil {
		return ResetPasswordOutput{}, err
	}
	return ResetPasswordOutput{AccessToken: sess.AccessToken, RefreshToken: sess.RefreshToken, KdfSalt: u.KdfSalt, UserID: u.ID, PasswordKeyEnvelope: u.PasswordKeyEnvelope, PasswordKeyNonce: u.PasswordKeyNonce, RecoveryKeyEnvelope: u.RecoveryKeyEnvelope, RecoveryKeyNonce: u.RecoveryKeyNonce}, nil
}
func (s *ServiceImpl) findByDestinationHash(ctx context.Context, h string) (*entity.User, error) {
	if u, e := s.users.FindByPhoneHash(ctx, h); e == nil {
		return u, nil
	}
	return s.users.FindByEmailHash(ctx, h)
}
func (s *ServiceImpl) issueSession(ctx context.Context, u *entity.User, d string) (RegisterOutput, error) {
	x, e := s.issueRefreshAndAccess(ctx, u, d)
	if e != nil {
		return RegisterOutput{}, e
	}
	return RegisterOutput{AccessToken: x.AccessToken, RefreshToken: x.RefreshToken, KdfSalt: u.KdfSalt, UserID: u.ID, RecoveryRequired: u.RecoveryKeyHash != ""}, nil
}

type sessionOutput struct{ AccessToken, RefreshToken string }

func (s *ServiceImpl) issueRefreshAndAccess(ctx context.Context, u *entity.User, d string) (sessionOutput, error) {
	a, e := infraauth.GenerateAccessToken(u.ID, s.accessTTL, s.secret)
	if e != nil {
		return sessionOutput{}, apperror.ErrInternal("failed to create access token")
	}
	raw, e := infraauth.GenerateRefreshToken()
	if e != nil {
		return sessionOutput{}, apperror.ErrInternal("failed to create refresh token")
	}
	now := time.Now().UTC()
	if e = s.refresh.Create(ctx, &entity.RefreshToken{TokenHash: infraauth.HashRefreshToken(raw), UserID: u.ID, DeviceID: d, CreatedAt: now, ExpiresAt: now.Add(s.refreshTTL)}); e != nil {
		return sessionOutput{}, e
	}
	return sessionOutput{a, raw}, nil
}
func validDevice(v string) error {
	v = strings.TrimSpace(v)
	if v == "" || len(v) > 128 {
		return apperror.ErrValidation("device id is required and must be at most 128 characters")
	}
	return nil
}
func normalizePhone(v string) (string, error) {
	v = strings.TrimSpace(v)
	if len(v) < 8 || len(v) > 15 || !strings.HasPrefix(v, "+") {
		return "", errors.New("invalid phone")
	}
	for _, r := range v[1:] {
		if r < '0' || r > '9' {
			return "", errors.New("invalid phone")
		}
	}
	return v, nil
}
func hashIdentifier(v, secret string) string {
	mac := hmac.New(sha256.New, []byte(secret))
	_, _ = mac.Write([]byte(strings.ToLower(strings.TrimSpace(v))))
	return base64.RawURLEncoding.EncodeToString(mac.Sum(nil))
}
func hashRecovery(v string) string {
	h := sha256.Sum256([]byte(strings.TrimSpace(v)))
	return base64.RawURLEncoding.EncodeToString(h[:])
}
