package auth

import (
	"context"
	"crypto/hmac"
	"crypto/sha256"
	"encoding/base64"
	"errors"
	"strings"
	"time"

	"github.com/mosinaRZ/finance-sync-backend/internal/domain/apperror"
	"github.com/mosinaRZ/finance-sync-backend/internal/domain/entity"
	"github.com/mosinaRZ/finance-sync-backend/internal/domain/repository"
	infraauth "github.com/mosinaRZ/finance-sync-backend/internal/infrastructure/auth"
)

type ServiceImpl struct {
	users                 repository.UserRepository
	refresh               repository.RefreshTokenRepository
	secret                string
	accessTTL, refreshTTL time.Duration
}

func NewService(users repository.UserRepository, refresh repository.RefreshTokenRepository, secret string, accessTTL, refreshTTL time.Duration) *ServiceImpl {
	return &ServiceImpl{users: users, refresh: refresh, secret: secret, accessTTL: accessTTL, refreshTTL: refreshTTL}
}

func (s *ServiceImpl) Register(ctx context.Context, in RegisterInput) (RegisterOutput, error) {
	phone, err := normalizePhone(in.PhoneNumber)
	if err != nil {
		return RegisterOutput{}, apperror.ErrValidation("invalid phone number")
	}
	if strings.TrimSpace(in.Password) == "" || len(in.Password) < 8 || len(in.Password) > 256 {
		return RegisterOutput{}, apperror.ErrValidation("password must be between 8 and 256 characters")
	}
	if strings.TrimSpace(in.DeviceID) == "" || len(in.DeviceID) > 128 {
		return RegisterOutput{}, apperror.ErrValidation("device id is required and must be at most 128 characters")
	}
	phoneHash := hashPhone(phone, s.secret)
	if _, err := s.users.FindByPhoneHash(ctx, phoneHash); err == nil {
		return RegisterOutput{}, apperror.ErrConflict("user already exists")
	} else if code, ok := apperror.CodeOf(err); !ok || code != apperror.CodeNotFound {
		return RegisterOutput{}, err
	}
	salt, err := infraauth.GenerateSalt()
	if err != nil {
		return RegisterOutput{}, apperror.ErrInternal("failed to prepare password")
	}
	passwordHash, err := infraauth.HashPassword(in.Password, salt)
	if err != nil {
		return RegisterOutput{}, apperror.ErrInternal("failed to hash password")
	}
	kdfSalt, err := infraauth.GenerateSalt()
	if err != nil {
		return RegisterOutput{}, apperror.ErrInternal("failed to prepare encryption salt")
	}
	now := time.Now().UTC()
	user := &entity.User{PhoneHash: phoneHash, PasswordHash: passwordHash, AuthSalt: base64.RawStdEncoding.EncodeToString(salt), KdfSalt: base64.RawStdEncoding.EncodeToString(kdfSalt), CreatedAt: now, UpdatedAt: now, Devices: []string{in.DeviceID}}
	if err := s.users.Create(ctx, user); err != nil {
		return RegisterOutput{}, err
	}
	return s.issueSession(ctx, user, in.DeviceID)
}

func (s *ServiceImpl) Login(ctx context.Context, in LoginInput) (LoginOutput, error) {
	if len(in.Password) > 256 {
		return LoginOutput{}, apperror.ErrUnauthorized("invalid credentials")
	}
	phone, err := normalizePhone(in.PhoneNumber)
	if err != nil {
		return LoginOutput{}, apperror.ErrUnauthorized("invalid credentials")
	}
	user, err := s.users.FindByPhoneHash(ctx, hashPhone(phone, s.secret))
	if err != nil {
		return LoginOutput{}, apperror.ErrUnauthorized("invalid credentials")
	}
	ok, verifyErr := infraauth.VerifyPassword(in.Password, user.AuthSalt, user.PasswordHash)
	if verifyErr != nil || !ok {
		return LoginOutput{}, apperror.ErrUnauthorized("invalid credentials")
	}
	if strings.TrimSpace(in.DeviceID) == "" || len(in.DeviceID) > 128 {
		return LoginOutput{}, apperror.ErrValidation("device id is required and must be at most 128 characters")
	}
	if err := s.users.AddDevice(ctx, user.ID, in.DeviceID); err != nil {
		return LoginOutput{}, err
	}
	session, err := s.issueRefreshAndAccess(ctx, user, in.DeviceID)
	if err != nil {
		return LoginOutput{}, err
	}
	return LoginOutput{AccessToken: session.AccessToken, RefreshToken: session.RefreshToken, KdfSalt: user.KdfSalt, UserID: user.ID}, nil
}

func (s *ServiceImpl) Refresh(ctx context.Context, in RefreshInput) (RefreshOutput, error) {
	if strings.TrimSpace(in.RefreshToken) == "" {
		return RefreshOutput{}, apperror.ErrUnauthorized("invalid refresh token")
	}
	hash := infraauth.HashRefreshToken(in.RefreshToken)
	old, err := s.refresh.FindByHash(ctx, hash)
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
	user, err := s.users.FindByID(ctx, old.UserID)
	if err != nil {
		return RefreshOutput{}, apperror.ErrUnauthorized("invalid refresh token")
	}
	if err := s.refresh.Revoke(ctx, hash); err != nil {
		if code, ok := apperror.CodeOf(err); ok && code == apperror.CodeConflict {
			_ = s.refresh.RevokeAllForUser(ctx, old.UserID)
			return RefreshOutput{}, apperror.ErrUnauthorized("refresh token reuse detected")
		}
		return RefreshOutput{}, err
	}
	session, err := s.issueRefreshAndAccess(ctx, user, old.DeviceID)
	if err != nil {
		return RefreshOutput{}, err
	}
	return RefreshOutput{AccessToken: session.AccessToken, RefreshToken: session.RefreshToken}, nil
}

func (s *ServiceImpl) issueSession(ctx context.Context, user *entity.User, deviceID string) (RegisterOutput, error) {
	session, err := s.issueRefreshAndAccess(ctx, user, deviceID)
	if err != nil {
		return RegisterOutput{}, err
	}
	return RegisterOutput{AccessToken: session.AccessToken, RefreshToken: session.RefreshToken, KdfSalt: user.KdfSalt, UserID: user.ID}, nil
}

type sessionOutput struct{ AccessToken, RefreshToken string }

func (s *ServiceImpl) issueRefreshAndAccess(ctx context.Context, user *entity.User, deviceID string) (sessionOutput, error) {
	access, err := infraauth.GenerateAccessToken(user.ID, s.accessTTL, s.secret)
	if err != nil {
		return sessionOutput{}, apperror.ErrInternal("failed to create access token")
	}
	raw, err := infraauth.GenerateRefreshToken()
	if err != nil {
		return sessionOutput{}, apperror.ErrInternal("failed to create refresh token")
	}
	now := time.Now().UTC()
	t := &entity.RefreshToken{TokenHash: infraauth.HashRefreshToken(raw), UserID: user.ID, DeviceID: deviceID, CreatedAt: now, ExpiresAt: now.Add(s.refreshTTL)}
	if err := s.refresh.Create(ctx, t); err != nil {
		return sessionOutput{}, err
	}
	return sessionOutput{AccessToken: access, RefreshToken: raw}, nil
}

func normalizePhone(value string) (string, error) {
	v := strings.TrimSpace(value)
	if len(v) < 8 || len(v) > 16 || !strings.HasPrefix(v, "+") {
		return "", errors.New("phone must be E.164")
	}
	for _, r := range v[1:] {
		if r < '0' || r > '9' {
			return "", errors.New("phone must be E.164")
		}
	}
	return v, nil
}

func hashPhone(phone, secret string) string {
	mac := hmac.New(sha256.New, []byte(secret))
	_, _ = mac.Write([]byte(phone))
	return base64.RawURLEncoding.EncodeToString(mac.Sum(nil))
}
