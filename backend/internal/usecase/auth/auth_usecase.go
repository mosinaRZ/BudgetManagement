package auth

import (
	"bytes"
	"context"
	"crypto/hmac"
	"crypto/sha256"
	"encoding/base64"
	"errors"
	"log/slog"
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

	// DefaultAccountRestoreWindow is how long a soft-deleted account can be restored by its owner.
	// After it the account is treated as gone and becomes eligible for permanent removal
	// (see cmd/purge-deleted).
	DefaultAccountRestoreWindow = 30 * 24 * time.Hour
)

var (
	dummyAuthSalt    = base64.RawStdEncoding.EncodeToString(bytes.Repeat([]byte{0x42}, 16))
	dummyAuthHash, _ = infraauth.HashPassword("cidna-invalid-user-dummy-password", bytes.Repeat([]byte{0x42}, 16))
)

type ServiceImpl struct {
	users                 repository.UserRepository
	refresh               repository.RefreshTokenRepository
	devices               repository.DeviceRepository
	deviceEvents          repository.DeviceEventRepository
	recoverySessions      repository.RecoverySessionRepository
	otp                   OTPService
	secret                string
	accessTTL, refreshTTL time.Duration
	restoreWindow         time.Duration
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

		restoreWindow: DefaultAccountRestoreWindow,
	}
}

// SetAccountRestoreWindow overrides how long a soft-deleted account stays restorable.
func (s *ServiceImpl) SetAccountRestoreWindow(d time.Duration) {
	if d > 0 {
		s.restoreWindow = d
	}
}

// restorable reports whether a soft-deleted account is still inside its grace period.
func (s *ServiceImpl) restorable(u *entity.User, now time.Time) bool {
	until, ok := u.RestorableUntil(s.restoreWindow)
	return ok && now.Before(until)
}
func (s *ServiceImpl) SetDeviceRepository(d repository.DeviceRepository) { s.devices = d }

// SetDeviceEventRepository enables sign-in events: when a device that was not yet part of the
// account signs in, the account's other devices are told so they can alert the user.
func (s *ServiceImpl) SetDeviceEventRepository(r repository.DeviceEventRepository) {
	s.deviceEvents = r
}
func (s *ServiceImpl) SetRecoverySessionRepository(r repository.RecoverySessionRepository) {
	s.recoverySessions = r
}
func (s *ServiceImpl) Register(ctx context.Context, in RegisterInput) (RegisterOutput, error) {
	phone, err := normalizePhone(in.PhoneNumber)
	if err != nil {
		return RegisterOutput{}, apperror.ErrValidation("invalid phone number")
	}

	// The client never sends the password: only an authentication secret derived from it on the
	// device (see infrastructure/auth/e2ee_envelope.go). Password strength is a client concern.
	authSecret, err := canonicalAuthSecret(in.AuthSecret)
	if err != nil {
		return RegisterOutput{}, err
	}
	if err := validDevice(in.DeviceID); err != nil {
		return RegisterOutput{}, err
	}
	if s.otp == nil {
		return RegisterOutput{}, apperror.ErrInternal("OTP service is not configured")
	}

	phoneHash := hashIdentifier(phone, s.secret)
	if err := s.verifyRegistrationOTP(ctx, in, phoneHash); err != nil {
		return RegisterOutput{}, err
	}

	if err := s.ensurePhoneAvailable(ctx, phoneHash); err != nil {
		return RegisterOutput{}, err
	}

	authSalt, err := infraauth.GenerateSalt()
	if err != nil {
		return RegisterOutput{}, apperror.ErrInternal("failed to generate password salt")
	}

	passwordHash, err := infraauth.HashPassword(authSecret, authSalt)
	if err != nil {
		return RegisterOutput{}, apperror.ErrInternal("failed to hash password")
	}

	kdfSalt, err := prepareRegistrationKdfSalt(in.KdfSalt)
	if err != nil {
		return RegisterOutput{}, err
	}

	if err := validateClientKeyMaterial(
		kdfSalt,
		in.PasswordKeyEnvelope,
		in.PasswordKeyNonce,
		in.RecoveryKeyHash,
		in.RecoveryKeyEnvelope,
		in.RecoveryKeyNonce,
	); err != nil {
		return RegisterOutput{}, err
	}

	now := time.Now().UTC()
	user := &entity.User{
		Role:                entity.RoleUser,
		PhoneHash:           phoneHash,
		PhoneNumber:         phone,
		PhoneVerified:       true,
		PasswordHash:        passwordHash,
		AuthSalt:            base64.RawStdEncoding.EncodeToString(authSalt),
		KdfSalt:             kdfSalt,
		PasswordKeyEnvelope: in.PasswordKeyEnvelope,
		PasswordKeyNonce:    in.PasswordKeyNonce,
		RecoveryKeyHash:     base64.RawURLEncoding.EncodeToString(in.RecoveryKeyHash),
		RecoveryKeyEnvelope: in.RecoveryKeyEnvelope,
		RecoveryKeyNonce:    in.RecoveryKeyNonce,
		CreatedAt:           now,
		UpdatedAt:           now,
		Devices:             []string{in.DeviceID},
	}

	if err := s.users.Create(ctx, user); err != nil {
		return RegisterOutput{}, err
	}

	if s.devices != nil {
		if err := s.devices.Register(ctx, newDeviceRecord(in.DeviceID, user.ID, now, in.Device)); err != nil {
			return RegisterOutput{}, err
		}
	}

	return s.issueSession(ctx, user, in.DeviceID)
}

func (s *ServiceImpl) verifyRegistrationOTP(ctx context.Context, in RegisterInput, phoneHash string) error {
	destination, err := s.otp.Verify(ctx, VerifyOTPInput{
		ChallengeID: in.OTPChallengeID,
		Code:        in.OTPCode,
		Purpose:     entity.OTPPurposeRegister,
	})
	if err != nil {
		return err
	}
	if !hmac.Equal([]byte(destination), []byte(phoneHash)) {
		return apperror.ErrUnauthorized("OTP destination does not match account identifier")
	}
	return nil
}

func (s *ServiceImpl) ensurePhoneAvailable(ctx context.Context, phoneHash string) error {
	existing, err := s.users.FindByPhoneHash(ctx, phoneHash)
	if err == nil && existing != nil {
		if existing.IsDeleted && s.restorable(existing, time.Now().UTC()) {
			return apperror.ErrConflict("this phone number belongs to a deleted account; sign in to restore it")
		}
		return apperror.ErrConflict("user already exists")
	}
	if err != nil {
		code, ok := apperror.CodeOf(err)
		if !ok || code != apperror.CodeNotFound {
			return err
		}
	}
	return nil
}

// prepareRegistrationKdfSalt requires a client-chosen salt: the client must know it before it
// can derive its authentication secret, so the server can no longer pick one on its behalf.
func prepareRegistrationKdfSalt(value string) (string, error) {
	decoded, err := decodeBase64Flexible(value)
	if err != nil || len(decoded) != infraauth.KDFSaltBytes {
		return "", apperror.ErrValidation("invalid KDF salt")
	}
	return value, nil
}

// canonicalAuthSecret validates the shape of an authentication secret (32 random-looking
// bytes, base64) and returns its canonical encoding so every spelling hashes identically.
// Rejecting anything else also stops a legacy client from sending a raw password.
func canonicalAuthSecret(v string) (string, error) {
	b, err := decodeBase64Flexible(v)
	if err != nil || len(b) != 32 {
		return "", apperror.ErrValidation("auth_secret must be a base64-encoded 32-byte value")
	}
	return base64.StdEncoding.EncodeToString(b), nil
}

// LoginParams returns the public KDF parameters a client needs before it can authenticate.
// Unknown identifiers receive a deterministic decoy salt, so the answer does not reveal
// whether an account exists.
func (s *ServiceImpl) LoginParams(ctx context.Context, identifier string) (LoginParamsOutput, error) {
	id := strings.TrimSpace(identifier)
	if id == "" || len(id) > 32 {
		return LoginParamsOutput{}, apperror.ErrValidation("identifier is required")
	}
	var u *entity.User
	if p, e := normalizePhone(id); e == nil {
		u, _ = s.users.FindByPhoneHash(ctx, hashIdentifier(p, s.secret))
	}
	salt := ""
	if u != nil {
		if b, err := decodeBase64Flexible(u.KdfSalt); err == nil && len(b) == infraauth.KDFSaltBytes {
			salt = base64.StdEncoding.EncodeToString(b)
		}
	}
	if salt == "" {
		mac := hmac.New(sha256.New, []byte(s.secret))
		_, _ = mac.Write([]byte("cidna/v2/kdf-salt|" + strings.ToLower(id)))
		salt = base64.StdEncoding.EncodeToString(mac.Sum(nil)[:infraauth.KDFSaltBytes])
	}
	return LoginParamsOutput{KdfSalt: salt, KdfIterations: infraauth.ClientPBKDF2Iterations, KdfVersion: infraauth.KDFVersion}, nil
}

func (s *ServiceImpl) Login(ctx context.Context, in LoginInput) (LoginOutput, error) {
	authSecret, err := canonicalAuthSecret(in.AuthSecret)
	if err != nil {
		return LoginOutput{}, err
	}
	id := strings.TrimSpace(in.Identifier)
	var u *entity.User
	{
		p, e := normalizePhone(id)
		if e == nil {
			u, err = s.users.FindByPhoneHash(ctx, hashIdentifier(p, s.secret))
		} else {
			// Keep malformed identifiers on the same expensive password-verification path.
			_, _ = infraauth.VerifyPassword(authSecret, dummyAuthSalt, dummyAuthHash)
			return LoginOutput{}, apperror.ErrUnauthorized("invalid credentials")
		}
	}
	if err != nil || u == nil {
		// Constant-cost dummy Argon2 verification prevents username/phone enumeration by timing.
		_, _ = infraauth.VerifyPassword(authSecret, dummyAuthSalt, dummyAuthHash)
		return LoginOutput{}, apperror.ErrUnauthorized("invalid credentials")
	}
	if u.LockedUntil != nil && u.LockedUntil.After(time.Now().UTC()) {
		// Do not reveal account-lock state; preserve the same public login error.
		return LoginOutput{}, apperror.ErrUnauthorized("invalid credentials")
	}
	ok, _ := infraauth.VerifyPassword(authSecret, u.AuthSalt, u.PasswordHash)
	if !ok {
		if err := s.users.RecordFailedLogin(ctx, u.ID, time.Now().UTC(), loginLockThreshold, loginLockDuration); err != nil {
			return LoginOutput{}, err
		}
		return LoginOutput{}, apperror.ErrUnauthorized("invalid credentials")
	}
	if err := s.users.ResetFailedLogin(ctx, u.ID); err != nil {
		return LoginOutput{}, err
	}
	if u.IsDeleted {
		// The password is correct, so revealing the deleted state leaks nothing to a stranger.
		now := time.Now().UTC()
		if !s.restorable(u, now) {
			return LoginOutput{}, apperror.ErrUnauthorized("invalid credentials")
		}
		if !in.RestoreAccount {
			return LoginOutput{}, apperror.ErrAccountDeleted("this account was deleted; sign in again with restore to recover it")
		}
		version, err := s.users.SetDeleted(ctx, u.ID, false, now)
		if err != nil {
			return LoginOutput{}, err
		}
		u.IsDeleted, u.DeletedAt, u.SessionVersion = false, nil, version
	}
	if err := validDevice(in.DeviceID); err != nil {
		return LoginOutput{}, err
	}
	if err := s.users.AddDevice(ctx, u.ID, in.DeviceID); err != nil {
		return LoginOutput{}, err
	}
	if err := s.registerDevice(ctx, u.ID, in.DeviceID, time.Now().UTC(), in.Device); err != nil {
		return LoginOutput{}, err
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
		// Removing a device revokes its refresh tokens too. When the removed device later tries
		// to refresh, that is not a replay of a stolen token and must not sign every other
		// device out: it just learns that it was removed. If the device still exists the token
		// was revoked by rotation or sign-out, so a second use is genuine reuse. When the
		// device state cannot be read, fall through to the stricter reuse handling.
		if s.devices != nil && old.DeviceID != "" {
			if exists, existsErr := s.devices.Exists(ctx, old.UserID, old.DeviceID); existsErr == nil && !exists {
				return RefreshOutput{}, apperror.ErrDeviceRemoved("this device has been removed from the account")
			}
		}
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
	if err != nil || u == nil || u.IsDeleted {
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
	// Signing out gives up this device's place on the account. Its row is removed so the remaining
	// devices can take over: the earliest-registered device left becomes the primary device.
	if s.devices != nil && token.DeviceID != "" {
		if err := s.refresh.RevokeByUserAndDevice(ctx, token.UserID, token.DeviceID); err != nil {
			return err
		}
		if err := s.devices.Revoke(ctx, token.UserID, token.DeviceID); err != nil {
			if code, ok := apperror.CodeOf(err); !ok || code != apperror.CodeNotFound {
				return err
			}
		}
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
	if err != nil || u == nil || u.IsDeleted {
		return PrepareRecoveryOutput{}, apperror.ErrUnauthorized("invalid recovery request")
	}
	if !verifyRecoveryProof(u.RecoveryKeyHash, in.RecoveryProof) {
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
	newAuthSecret, err := canonicalAuthSecret(in.NewAuthSecret)
	if err != nil {
		return ResetPasswordOutput{}, err
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
	if err != nil || u == nil || u.IsDeleted {
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
	ph, err := infraauth.HashPassword(newAuthSecret, salt)
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
	if err := s.registerDevice(ctx, u.ID, in.DeviceID, time.Now().UTC(), in.Device); err != nil {
		return ResetPasswordOutput{}, err
	}
	sess, err := s.issueRefreshAndAccess(ctx, u, in.DeviceID)
	if err != nil {
		return ResetPasswordOutput{}, err
	}
	return ResetPasswordOutput{AccessToken: sess.AccessToken, RefreshToken: sess.RefreshToken, Role: u.Role, KdfSalt: u.KdfSalt, UserID: u.ID, PasswordKeyEnvelope: u.PasswordKeyEnvelope, PasswordKeyNonce: u.PasswordKeyNonce, RecoveryKeyEnvelope: u.RecoveryKeyEnvelope, RecoveryKeyNonce: u.RecoveryKeyNonce}, nil
}

// ChangePassword replaces the password of an authenticated user.
//
// The current password is always re-verified (a stolen access token alone must not be
// enough to take over an account) and wrong attempts count towards the same lockout as
// sign-in. Because the data key is wrapped client-side, the client sends the freshly
// wrapped envelope; the server stores it opaquely, rotates the credential hash, revokes
// every existing session and returns a new session for the calling device only.
func (s *ServiceImpl) ChangePassword(ctx context.Context, in ChangePasswordInput) (ChangePasswordOutput, error) {
	if strings.TrimSpace(in.UserID) == "" {
		return ChangePasswordOutput{}, apperror.ErrUnauthorized("invalid session")
	}
	if s.devices == nil {
		return ChangePasswordOutput{}, apperror.ErrInternal("device service is not configured")
	}
	if err := validDevice(in.DeviceID); err != nil {
		return ChangePasswordOutput{}, err
	}
	newAuthSecret, err := canonicalAuthSecret(in.NewAuthSecret)
	if err != nil {
		return ChangePasswordOutput{}, err
	}
	currentAuthSecret, err := canonicalAuthSecret(in.CurrentAuthSecret)
	if err != nil {
		return ChangePasswordOutput{}, err
	}
	// The account KDF salt is stable, so identical secrets mean identical passwords.
	if newAuthSecret == currentAuthSecret {
		return ChangePasswordOutput{}, apperror.ErrValidation("new password must differ from the current password")
	}
	newKdfSalt, err := decodeAndValidateKdfSalt(in.KdfSalt)
	if err != nil {
		return ChangePasswordOutput{}, apperror.ErrValidation("invalid KDF salt")
	}
	if err := validatePasswordKeyMaterial(in.PasswordKeyEnvelope, in.PasswordKeyNonce); err != nil {
		return ChangePasswordOutput{}, err
	}

	u, err := s.users.FindByID(ctx, in.UserID)
	if err != nil {
		if code, ok := apperror.CodeOf(err); ok && code == apperror.CodeNotFound {
			return ChangePasswordOutput{}, apperror.ErrUnauthorized("invalid session")
		}
		return ChangePasswordOutput{}, err
	}
	if u == nil || u.IsDeleted {
		return ChangePasswordOutput{}, apperror.ErrUnauthorized("invalid session")
	}

	now := time.Now().UTC()
	if u.LockedUntil != nil && u.LockedUntil.After(now) {
		// Refuse before spending an Argon2 verification on a locked account.
		return ChangePasswordOutput{}, apperror.ErrRateLimited("too many failed attempts; try again later")
	}

	storedKdfSalt, err := decodeAndValidateKdfSalt(u.KdfSalt)
	if err != nil || !bytes.Equal(storedKdfSalt, newKdfSalt) {
		return ChangePasswordOutput{}, apperror.ErrValidation("KDF salt must match the account salt")
	}

	registered, err := s.devices.Exists(ctx, u.ID, in.DeviceID)
	if err != nil {
		return ChangePasswordOutput{}, err
	}
	if !registered {
		return ChangePasswordOutput{}, apperror.ErrForbidden("device is not registered")
	}

	ok, _ := infraauth.VerifyPassword(currentAuthSecret, u.AuthSalt, u.PasswordHash)
	if !ok {
		if err := s.users.RecordFailedLogin(ctx, u.ID, now, loginLockThreshold, loginLockDuration); err != nil {
			return ChangePasswordOutput{}, err
		}
		return ChangePasswordOutput{}, apperror.ErrInvalidPassword("current password is incorrect")
	}
	if err := s.users.ResetFailedLogin(ctx, u.ID); err != nil {
		return ChangePasswordOutput{}, err
	}

	salt, err := infraauth.GenerateSalt()
	if err != nil {
		return ChangePasswordOutput{}, apperror.ErrInternal("failed to generate password salt")
	}
	passwordHash, err := infraauth.HashPassword(newAuthSecret, salt)
	if err != nil {
		return ChangePasswordOutput{}, apperror.ErrInternal("failed to hash password")
	}
	newAuthSalt := base64.RawStdEncoding.EncodeToString(salt)
	newKdfSaltB64 := base64.RawStdEncoding.EncodeToString(newKdfSalt)
	if err := s.users.UpdateCredentials(ctx, u.ID, passwordHash, newAuthSalt, newKdfSaltB64, in.PasswordKeyEnvelope, in.PasswordKeyNonce); err != nil {
		return ChangePasswordOutput{}, err
	}
	u.PasswordHash = passwordHash
	u.AuthSalt = newAuthSalt
	u.KdfSalt = newKdfSaltB64
	u.PasswordKeyEnvelope = in.PasswordKeyEnvelope
	u.PasswordKeyNonce = in.PasswordKeyNonce

	newSessionVersion, err := s.users.IncrementSessionVersion(ctx, u.ID)
	if err != nil {
		return ChangePasswordOutput{}, err
	}
	u.SessionVersion = newSessionVersion
	if err := s.refresh.RevokeAllForUser(ctx, u.ID); err != nil {
		return ChangePasswordOutput{}, err
	}
	if err := s.devices.Register(ctx, newDeviceRecord(in.DeviceID, u.ID, now, in.Device)); err != nil {
		return ChangePasswordOutput{}, err
	}

	sess, err := s.issueRefreshAndAccess(ctx, u, in.DeviceID)
	if err != nil {
		return ChangePasswordOutput{}, err
	}
	return ChangePasswordOutput{AccessToken: sess.AccessToken, RefreshToken: sess.RefreshToken, Role: u.Role, KdfSalt: u.KdfSalt, UserID: u.ID, PasswordKeyEnvelope: u.PasswordKeyEnvelope, PasswordKeyNonce: u.PasswordKeyNonce, RecoveryKeyEnvelope: u.RecoveryKeyEnvelope, RecoveryKeyNonce: u.RecoveryKeyNonce}, nil
}

// DeleteAccount soft-deletes the caller's account (isDeleted=true).
//
// The password is always re-verified and wrong attempts count towards the sign-in lockout. On
// success every access token is invalidated (session version bump) and every refresh token is
// revoked. The data stays on the server, untouched, until the grace period ends so the owner can
// restore the account by signing in with the restore flag; afterwards cmd/purge-deleted removes it.
func (s *ServiceImpl) DeleteAccount(ctx context.Context, in DeleteAccountInput) (DeleteAccountOutput, error) {
	if strings.TrimSpace(in.UserID) == "" {
		return DeleteAccountOutput{}, apperror.ErrUnauthorized("invalid session")
	}
	authSecret, err := canonicalAuthSecret(in.AuthSecret)
	if err != nil {
		return DeleteAccountOutput{}, err
	}
	u, err := s.users.FindByID(ctx, in.UserID)
	if err != nil {
		if code, ok := apperror.CodeOf(err); ok && code == apperror.CodeNotFound {
			return DeleteAccountOutput{}, apperror.ErrUnauthorized("invalid session")
		}
		return DeleteAccountOutput{}, err
	}
	if u == nil || u.IsDeleted {
		return DeleteAccountOutput{}, apperror.ErrUnauthorized("invalid session")
	}
	now := time.Now().UTC()
	if u.LockedUntil != nil && u.LockedUntil.After(now) {
		return DeleteAccountOutput{}, apperror.ErrRateLimited("too many failed attempts; try again later")
	}
	ok, _ := infraauth.VerifyPassword(authSecret, u.AuthSalt, u.PasswordHash)
	if !ok {
		if err := s.users.RecordFailedLogin(ctx, u.ID, now, loginLockThreshold, loginLockDuration); err != nil {
			return DeleteAccountOutput{}, err
		}
		return DeleteAccountOutput{}, apperror.ErrInvalidPassword("password is incorrect")
	}
	if err := s.users.ResetFailedLogin(ctx, u.ID); err != nil {
		return DeleteAccountOutput{}, err
	}
	if _, err := s.users.SetDeleted(ctx, u.ID, true, now); err != nil {
		return DeleteAccountOutput{}, err
	}
	// The account is already unusable (session version bumped, Refresh rejects deleted users), so
	// a failure here must not turn a successful deletion into an error the client would retry.
	if err := s.refresh.RevokeAllForUser(ctx, u.ID); err != nil {
		slog.Warn("failed to revoke refresh tokens of deleted account", "user_id", u.ID, "error", err)
	}
	return DeleteAccountOutput{DeletedAt: now, RestorableUntil: now.Add(s.restoreWindow)}, nil
}

func (s *ServiceImpl) GetProfile(ctx context.Context, userID string) (Profile, error) {
	if strings.TrimSpace(userID) == "" {
		return Profile{}, apperror.ErrUnauthorized("authentication required")
	}
	u, err := s.users.FindByID(ctx, userID)
	if err != nil {
		return Profile{}, err
	}
	if u == nil || u.IsDeleted {
		return Profile{}, apperror.ErrUnauthorized("invalid session")
	}
	return profileFromUser(u), nil
}

func (s *ServiceImpl) UpdateProfile(ctx context.Context, in UpdateProfileInput) (Profile, error) {
	if strings.TrimSpace(in.UserID) == "" {
		return Profile{}, apperror.ErrUnauthorized("authentication required")
	}
	first := strings.TrimSpace(in.FirstName)
	last := strings.TrimSpace(in.LastName)
	if len([]rune(first)) > 80 || len([]rune(last)) > 80 {
		return Profile{}, apperror.ErrValidation("name fields must be at most 80 characters")
	}
	if !in.Gender.Valid() {
		return Profile{}, apperror.ErrValidation("invalid gender")
	}
	if len(in.BirthDate) > 10 {
		return Profile{}, apperror.ErrValidation("invalid birth date")
	}
	if in.BirthDate != "" {
		parsed, err := time.Parse("2006-01-02", in.BirthDate)
		if err != nil {
			return Profile{}, apperror.ErrValidation("birth date must use YYYY-MM-DD")
		}
		if parsed.After(time.Now().UTC()) {
			return Profile{}, apperror.ErrValidation("birth date cannot be in the future")
		}
	}
	if err := s.users.UpdateProfile(ctx, in.UserID, first, last, in.Gender, in.BirthDate); err != nil {
		return Profile{}, err
	}
	u, err := s.users.FindByID(ctx, in.UserID)
	if err != nil {
		return Profile{}, err
	}
	return profileFromUser(u), nil
}

func profileFromUser(u *entity.User) Profile {
	gender := u.Gender
	if !gender.Valid() {
		gender = entity.GenderPreferNotToSay
	}
	return Profile{
		UserID: u.ID, PhoneNumber: u.PhoneNumber,
		FirstName: u.FirstName, LastName: u.LastName, Gender: gender, BirthDate: u.BirthDate,
	}
}

// registerDevice stores the device row for a successful sign-in. When the device was not yet
// part of the account it also records a sign-in event, which is how the account's other
// devices learn about it. Recording is best-effort: it must never block the sign-in itself.
func (s *ServiceImpl) registerDevice(ctx context.Context, userID, deviceID string, now time.Time, info DeviceInfo) error {
	if s.devices == nil {
		return nil
	}
	record := newDeviceRecord(deviceID, userID, now, info)
	known := true
	if s.deviceEvents != nil {
		exists, err := s.devices.Exists(ctx, userID, deviceID)
		if err != nil {
			return err
		}
		// A reinstalled app that comes back under a new device id is the same device, not a new one.
		known = exists || s.sameInstallationStored(ctx, userID, deviceID, record.Fingerprint)
	}
	if err := s.devices.Register(ctx, record); err != nil {
		return err
	}
	if known || s.deviceEvents == nil {
		return nil
	}
	event := &entity.DeviceEvent{
		UserID:        userID,
		Type:          entity.DeviceEventSignedIn,
		DeviceID:      deviceID,
		DeviceName:    record.DisplayName(),
		Platform:      record.Platform,
		ActorDeviceID: deviceID,
		ActorName:     record.DisplayName(),
		CreatedAt:     now,
	}
	if err := s.deviceEvents.Record(ctx, event); err != nil {
		slog.Warn("failed to record device sign-in event", "user_id", userID, "device_id", deviceID, "error", err)
	}
	return nil
}

// sameInstallationStored reports whether the account already holds a row for the same installation
// under a different device id, which happens after a reinstall or a cleared-data reset.
func (s *ServiceImpl) sameInstallationStored(ctx context.Context, userID, deviceID, fingerprint string) bool {
	if fingerprint == "" {
		return false
	}
	list, err := s.devices.List(ctx, userID)
	if err != nil {
		return false
	}
	for _, d := range list {
		if d != nil && d.ID != deviceID && d.Fingerprint == fingerprint {
			return true
		}
	}
	return false
}

// newDeviceRecord builds the device row for a successful authentication. Descriptive
// values come from the client and are sanitised by ApplyMetadata.
func newDeviceRecord(deviceID, userID string, now time.Time, info DeviceInfo) *entity.Device {
	d := &entity.Device{ID: deviceID, UserID: userID, CreatedAt: now, LastSeenAt: now}
	d.ApplyMetadata(info.Name, info.Model, info.Platform, info.OSVersion, info.AppVersion, info.IP)
	d.SetFingerprint(info.Fingerprint)
	return d
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

	return nil, apperror.ErrNotFound("user not found")
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
	// Accounts created before roles existed have no role stored. They are regular users; refusing
	// to issue a token for them would lock legacy users out of their own account.
	if !u.Role.Valid() {
		u.Role = entity.RoleUser
	}
	a, e := infraauth.GenerateAccessTokenForDevice(u.ID, u.Role, u.SessionVersion, d, s.accessTTL, s.secret)
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
	if len(v) < 8 || len(v) > 16 || !strings.HasPrefix(v, "+") {
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

// verifyRecoveryProof checks a client-derived recovery proof against the stored SHA-256 of the
// expected proof. The recovery key itself never reaches the server (it can unwrap the data key).
func verifyRecoveryProof(stored, proof string) bool {
	if stored == "" {
		return false
	}
	raw, err := decodeBase64Flexible(proof)
	if err != nil || len(raw) != 32 {
		return false
	}
	want, err := decodeBase64Flexible(stored)
	if err != nil || len(want) != sha256.Size {
		return false
	}
	got := infraauth.RecoveryKeyHashFromProof(raw)
	return hmac.Equal(got, want)
}

// IdentifierHashForCLI hashes a normalized identifier using the same server secret used by authentication.
func IdentifierHashForCLI(identifier, secret string) string {
	return hashIdentifier(identifier, secret)
}

// NormalizePhoneForCLI exposes the same validation used by login/register to maintenance tooling.
func NormalizePhoneForCLI(identifier string) (string, error) { return normalizePhone(identifier) }
