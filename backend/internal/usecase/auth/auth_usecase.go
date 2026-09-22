package auth

import (
	"bytes"
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

const (
	loginLockThreshold = 5
	loginLockDuration  = 15 * time.Minute
)

var (
	dummyAuthSalt    = base64.RawStdEncoding.EncodeToString(bytes.Repeat([]byte{0x42}, 16))
	dummyAuthHash, _ = infraauth.HashPassword("cidna-invalid-user-dummy-password", bytes.Repeat([]byte{0x42}, 16))
)

type ServiceImpl struct {
	users                 repository.UserRepository
	refresh               repository.RefreshTokenRepository
	devices               repository.DeviceRepository
	recoverySessions      repository.RecoverySessionRepository
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
func (s *ServiceImpl) SetRecoverySessionRepository(r repository.RecoverySessionRepository) {
	s.recoverySessions = r
}
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
	if existing, err := s.users.FindByPhoneHash(ctx, ph); err == nil && existing != nil {
		return RegisterOutput{}, apperror.ErrConflict("user already exists")
	} else if err != nil {
		code, ok := apperror.CodeOf(err)
		if !ok || code != apperror.CodeNotFound {
			return RegisterOutput{}, err
		}
	}
	emailHash := ""
	emailVerified := false
	if strings.TrimSpace(in.Email) != "" {
		emailHash = hashIdentifier(strings.ToLower(strings.TrimSpace(in.Email)), s.secret)
		if existing, err := s.users.FindByEmailHash(ctx, emailHash); err == nil && existing != nil {
			return RegisterOutput{}, apperror.ErrConflict("email already exists")
		} else if err != nil {
			code, ok := apperror.CodeOf(err)
			if !ok || code != apperror.CodeNotFound {
				return RegisterOutput{}, err
			}
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
	salt, err := infraauth.GenerateSalt()
	if err != nil {
		return RegisterOutput{}, apperror.ErrInternal("failed to generate password salt")
	}
	passwordHash, err := infraauth.HashPassword(in.Password, salt)
	if err != nil {
		return RegisterOutput{}, apperror.ErrInternal("failed to hash password")
	}
	kdfSalt := in.KdfSalt
	if kdfSalt != "" {
		decoded, decodeErr := decodeBase64Flexible(kdfSalt)
		if decodeErr != nil || len(decoded) != 16 {
			return RegisterOutput{}, apperror.ErrValidation("invalid KDF salt")
		}
	} else {
		b, err := infraauth.GenerateSalt()
		if err != nil {
			return RegisterOutput{}, apperror.ErrInternal("failed to generate KDF salt")
		}
		kdfSalt = base64.RawStdEncoding.EncodeToString(b)
	}
	if err := validateClientKeyMaterial(kdfSalt, in.PasswordKeyEnvelope, in.PasswordKeyNonce, in.RecoveryKeyHash, in.RecoveryKeyEnvelope, in.RecoveryKeyNonce); err != nil {
		return RegisterOutput{}, err
	}
	now := time.Now().UTC()
	u := &entity.User{Role: entity.RoleUser, PhoneHash: ph, EmailHash: emailHash, PhoneVerified: true, EmailVerified: emailVerified, PasswordHash: passwordHash, AuthSalt: base64.RawStdEncoding.EncodeToString(salt), KdfSalt: kdfSalt, PasswordKeyEnvelope: in.PasswordKeyEnvelope, PasswordKeyNonce: in.PasswordKeyNonce, RecoveryKeyHash: string(in.RecoveryKeyHash), RecoveryKeyEnvelope: in.RecoveryKeyEnvelope, RecoveryKeyNonce: in.RecoveryKeyNonce, CreatedAt: now, UpdatedAt: now, Devices: []string{in.DeviceID}}
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
		if e == nil {
			u, err = s.users.FindByPhoneHash(ctx, hashIdentifier(p, s.secret))
		} else {
			// Keep malformed identifiers on the same expensive password-verification path.
			_, _ = infraauth.VerifyPassword(in.Password, dummyAuthSalt, dummyAuthHash)
			return LoginOutput{}, apperror.ErrUnauthorized("invalid credentials")
		}
	}
	if err != nil || u == nil {
		// Constant-cost dummy Argon2 verification prevents username/phone enumeration by timing.
		_, _ = infraauth.VerifyPassword(in.Password, dummyAuthSalt, dummyAuthHash)
		return LoginOutput{}, apperror.ErrUnauthorized("invalid credentials")
	}
	if u.LockedUntil != nil && u.LockedUntil.After(time.Now().UTC()) {
		// Do not reveal account-lock state; preserve the same public login error.
		return LoginOutput{}, apperror.ErrUnauthorized("invalid credentials")
	}
	ok, _ := infraauth.VerifyPassword(in.Password, u.AuthSalt, u.PasswordHash)
	if !ok {
		if err := s.users.RecordFailedLogin(ctx, u.ID, time.Now().UTC(), loginLockThreshold, loginLockDuration); err != nil {
			return LoginOutput{}, err
		}
		return LoginOutput{}, apperror.ErrUnauthorized("invalid credentials")
	}
	if err := s.users.ResetFailedLogin(ctx, u.ID); err != nil {
		return LoginOutput{}, err
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
	return LoginOutput{AccessToken: sess.AccessToken, RefreshToken: sess.RefreshToken, Role: u.Role, KdfSalt: u.KdfSalt, UserID: u.ID, PasswordKeyEnvelope: u.PasswordKeyEnvelope, PasswordKeyNonce: u.PasswordKeyNonce, RecoveryKeyEnvelope: u.RecoveryKeyEnvelope, RecoveryKeyNonce: u.RecoveryKeyNonce}, nil
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
		_, _ = s.users.IncrementSessionVersion(ctx, old.UserID)

		return RefreshOutput{}, apperror.ErrUnauthorized(
			"refresh token reuse detected",
		)
	}

	if !time.Now().Before(old.ExpiresAt) {
		return RefreshOutput{}, apperror.ErrUnauthorized(
			"refresh token expired",
		)
	}

	u, err := s.users.FindByID(ctx, old.UserID)
	if err != nil {
		return RefreshOutput{}, apperror.ErrUnauthorized(
			"invalid refresh token",
		)
	}

	if err := s.refresh.Revoke(ctx, h); err != nil {
		// A concurrent refresh may have revoked the same token after our read.
		// Treat that as refresh-token reuse and invalidate every session.
		if code, ok := apperror.CodeOf(err); ok && code == apperror.CodeConflict {
			_ = s.refresh.RevokeAllForUser(ctx, old.UserID)
			_, _ = s.users.IncrementSessionVersion(ctx, old.UserID)

			return RefreshOutput{}, apperror.ErrUnauthorized(
				"refresh token reuse detected",
			)
		}

		return RefreshOutput{}, err
	}

	sess, err := s.issueRefreshAndAccess(ctx, u, old.DeviceID)
	if err != nil {
		return RefreshOutput{}, err
	}

	return RefreshOutput{
		AccessToken:  sess.AccessToken,
		RefreshToken: sess.RefreshToken,
		Role:         u.Role,
	}, nil
}
func (s *ServiceImpl) Logout(ctx context.Context, raw string) error {
	if strings.TrimSpace(raw) == "" {
		return apperror.ErrUnauthorized("invalid refresh token")
	}
	hash := infraauth.HashRefreshToken(raw)
	token, err := s.refresh.FindByHash(ctx, hash)
	if err != nil {
		return apperror.ErrUnauthorized("invalid refresh token")
	}
	if err := s.refresh.Revoke(ctx, hash); err != nil {
		if code, ok := apperror.CodeOf(err); !ok || code != apperror.CodeConflict {
			return err
		}
	}
	if _, err := s.users.IncrementSessionVersion(ctx, token.UserID); err != nil {
		return err
	}
	return nil
}
func (s *ServiceImpl) PrepareRecovery(ctx context.Context, in PrepareRecoveryInput) (PrepareRecoveryOutput, error) {
	if s.recoverySessions == nil || s.otp == nil {
		return PrepareRecoveryOutput{}, apperror.ErrInternal("recovery service is not configured")
	}
	dest, err := s.otp.Verify(ctx, VerifyOTPInput{ChallengeID: in.ChallengeID, Code: in.OTPCode, Purpose: entity.OTPPurposePasswordReset})
	if err != nil {
		return PrepareRecoveryOutput{}, err
	}
	u, err := s.findByDestinationHash(ctx, dest)
	if err != nil {
		return PrepareRecoveryOutput{}, apperror.ErrUnauthorized("invalid recovery request")
	}
	if u.RecoveryKeyHash == "" || !hmac.Equal([]byte(u.RecoveryKeyHash), []byte(hashRecovery(in.RecoveryKey))) {
		return PrepareRecoveryOutput{}, apperror.ErrUnauthorized("recovery key is invalid")
	}
	raw, err := infraauth.GenerateRefreshToken()
	if err != nil {
		return PrepareRecoveryOutput{}, apperror.ErrInternal("failed to create recovery session")
	}
	now := time.Now().UTC()
	if err := s.recoverySessions.Create(ctx, &entity.RecoverySession{TokenHash: infraauth.HashRefreshToken(raw), UserID: u.ID, CreatedAt: now, ExpiresAt: now.Add(5 * time.Minute)}); err != nil {
		return PrepareRecoveryOutput{}, err
	}
	return PrepareRecoveryOutput{
		RecoverySessionToken: raw,
		KdfSalt:              u.KdfSalt,
		UserID:               u.ID,
		RecoveryKeyEnvelope:  append([]byte(nil), u.RecoveryKeyEnvelope...),
		RecoveryKeyNonce:     append([]byte(nil), u.RecoveryKeyNonce...),
	}, nil
}

func (s *ServiceImpl) ResetPassword(ctx context.Context, in ResetPasswordInput) (ResetPasswordOutput, error) {
	if s.recoverySessions == nil {
		return ResetPasswordOutput{}, apperror.ErrInternal("recovery service is not configured")
	}
	if len(in.NewPassword) < 8 || len(in.NewPassword) > 256 {
		return ResetPasswordOutput{}, apperror.ErrValidation("password must be between 8 and 256 characters")
	}
	if err := validDevice(in.DeviceID); err != nil {
		return ResetPasswordOutput{}, err
	}
	newKdfSalt, err := decodeAndValidateKdfSalt(in.KdfSalt)
	if err != nil {
		return ResetPasswordOutput{}, apperror.ErrValidation("invalid KDF salt")
	}
	if err := validatePasswordKeyMaterial(in.PasswordKeyEnvelope, in.PasswordKeyNonce); err != nil {
		return ResetPasswordOutput{}, err
	}
	recoverySession, err := s.recoverySessions.Consume(ctx, infraauth.HashRefreshToken(in.RecoverySessionToken), time.Now().UTC())
	if err != nil {
		return ResetPasswordOutput{}, err
	}
	u, err := s.users.FindByID(ctx, recoverySession.UserID)
	if err != nil {
		return ResetPasswordOutput{}, apperror.ErrUnauthorized("invalid recovery request")
	}
	storedKdfSalt, err := decodeAndValidateKdfSalt(u.KdfSalt)
	if err != nil || !bytes.Equal(storedKdfSalt, newKdfSalt) {
		return ResetPasswordOutput{}, apperror.ErrValidation("recovery KDF salt must match the account salt")
	}
	salt, err := infraauth.GenerateSalt()
	if err != nil {
		return ResetPasswordOutput{}, apperror.ErrInternal("failed to generate password salt")
	}
	ph, err := infraauth.HashPassword(in.NewPassword, salt)
	if err != nil {
		return ResetPasswordOutput{}, apperror.ErrInternal("failed to hash password")
	}
	if err := s.users.AddDevice(ctx, u.ID, in.DeviceID); err != nil {
		return ResetPasswordOutput{}, err
	}
	newAuthSalt := base64.RawStdEncoding.EncodeToString(salt)
	newKdfSaltB64 := base64.RawStdEncoding.EncodeToString(newKdfSalt)
	if err := s.users.UpdateCredentials(ctx, u.ID, ph, newAuthSalt, newKdfSaltB64, in.PasswordKeyEnvelope, in.PasswordKeyNonce); err != nil {
		return ResetPasswordOutput{}, err
	}
	u.PasswordHash = ph
	u.AuthSalt = newAuthSalt
	u.KdfSalt = newKdfSaltB64
	u.PasswordKeyEnvelope = in.PasswordKeyEnvelope
	u.PasswordKeyNonce = in.PasswordKeyNonce
	newSessionVersion, err := s.users.IncrementSessionVersion(ctx, u.ID)
	if err != nil {
		return ResetPasswordOutput{}, err
	}
	u.SessionVersion = newSessionVersion
	if err := s.refresh.RevokeAllForUser(ctx, u.ID); err != nil {
		return ResetPasswordOutput{}, err
	}
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
	return ResetPasswordOutput{AccessToken: sess.AccessToken, RefreshToken: sess.RefreshToken, Role: u.Role, KdfSalt: u.KdfSalt, UserID: u.ID, PasswordKeyEnvelope: u.PasswordKeyEnvelope, PasswordKeyNonce: u.PasswordKeyNonce, RecoveryKeyEnvelope: u.RecoveryKeyEnvelope, RecoveryKeyNonce: u.RecoveryKeyNonce}, nil
}
func (s *ServiceImpl) findByDestinationHash(ctx context.Context, h string) (*entity.User, error) {
	if u, err := s.users.FindByPhoneHash(ctx, h); err == nil {
		return u, nil
	} else {
		code, ok := apperror.CodeOf(err)
		if !ok || code != apperror.CodeNotFound {
			return nil, err
		}
	}

	return s.users.FindByEmailHash(ctx, h)
}
func (s *ServiceImpl) issueSession(ctx context.Context, u *entity.User, d string) (RegisterOutput, error) {
	x, e := s.issueRefreshAndAccess(ctx, u, d)
	if e != nil {
		return RegisterOutput{}, e
	}
	return RegisterOutput{AccessToken: x.AccessToken, RefreshToken: x.RefreshToken, Role: u.Role, KdfSalt: u.KdfSalt, UserID: u.ID, RecoveryRequired: u.RecoveryKeyHash != "", PasswordKeyEnvelope: u.PasswordKeyEnvelope, PasswordKeyNonce: u.PasswordKeyNonce, RecoveryKeyEnvelope: u.RecoveryKeyEnvelope, RecoveryKeyNonce: u.RecoveryKeyNonce}, nil
}

type sessionOutput struct{ AccessToken, RefreshToken string }

func (s *ServiceImpl) issueRefreshAndAccess(ctx context.Context, u *entity.User, d string) (sessionOutput, error) {
	a, e := infraauth.GenerateAccessTokenWithRoleAndSession(u.ID, u.Role, u.SessionVersion, s.accessTTL, s.secret)
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
func validateClientKeyMaterial(
	kdfSalt string,
	passwordEnvelope []byte,
	passwordNonce []byte,
	recoveryHash []byte,
	recoveryEnvelope []byte,
	recoveryNonce []byte,
) error {
	if _, err := decodeAndValidateKdfSalt(kdfSalt); err != nil {
		return apperror.ErrValidation("invalid KDF salt")
	}

	if err := validatePasswordKeyMaterial(passwordEnvelope, passwordNonce); err != nil {
		return err
	}

	if len(recoveryHash) != 32 {
		return apperror.ErrValidation("invalid recovery key hash")
	}

	if len(recoveryEnvelope) != 48 {
		return apperror.ErrValidation("invalid recovery key envelope")
	}

	if len(recoveryNonce) != 12 {
		return apperror.ErrValidation("invalid recovery key nonce")
	}

	return nil
}

func validatePasswordKeyMaterial(
	envelope []byte,
	nonce []byte,
) error {
	if len(envelope) != 48 {
		return apperror.ErrValidation("invalid password key envelope")
	}

	if len(nonce) != 12 {
		return apperror.ErrValidation("invalid password key nonce")
	}

	return nil
}

func decodeBase64Flexible(value string) ([]byte, error) {
	v := strings.TrimSpace(value)
	decoders := []*base64.Encoding{base64.StdEncoding, base64.RawStdEncoding, base64.URLEncoding, base64.RawURLEncoding}
	for _, decoder := range decoders {
		if b, err := decoder.DecodeString(v); err == nil {
			return b, nil
		}
	}
	return nil, errors.New("invalid base64")
}

func decodeAndValidateKdfSalt(value string) ([]byte, error) {
	b, err := decodeBase64Flexible(value)
	if err != nil || len(b) != 16 {
		return nil, errors.New("invalid KDF salt")
	}
	return b, nil
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
	v = strings.TrimSpace(v)
	// Recovery keys generated by the client are base64-encoded random bytes.
	// Hash the decoded bytes so the stored registration hash and recovery check
	// use the same canonical representation. Keep a string fallback for legacy
	// installations that may have stored the old representation.
	if raw, err := base64.RawStdEncoding.DecodeString(v); err == nil && len(raw) > 0 {
		h := sha256.Sum256(raw)
		return base64.RawURLEncoding.EncodeToString(h[:])
	}
	h := sha256.Sum256([]byte(v))
	return base64.RawURLEncoding.EncodeToString(h[:])
}

// IdentifierHashForCLI hashes a normalized identifier using the same server secret used by authentication.
func IdentifierHashForCLI(identifier, secret string) string {
	return hashIdentifier(identifier, secret)
}

// NormalizePhoneForCLI exposes the same validation used by login/register to maintenance tooling.
func NormalizePhoneForCLI(identifier string) (string, error) { return normalizePhone(identifier) }
