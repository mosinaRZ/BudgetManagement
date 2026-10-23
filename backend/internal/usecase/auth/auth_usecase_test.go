package auth

import (
	"context"
	"encoding/base64"
	"testing"
	"time"

	"github.com/mosinaRZ/finance-sync-backend/internal/domain/apperror"
	"github.com/mosinaRZ/finance-sync-backend/internal/domain/entity"
	infraauth "github.com/mosinaRZ/finance-sync-backend/internal/infrastructure/auth"
	"github.com/mosinaRZ/finance-sync-backend/internal/usecase/mocks"
)

const testSecret = "01234567890123456789012345678901"

type fakeOTPService struct {
	verify func(context.Context, VerifyOTPInput) (string, error)
}

func (f fakeOTPService) Request(context.Context, RequestOTPInput) (RequestOTPOutput, error) {
	return RequestOTPOutput{}, nil
}

func (f fakeOTPService) Verify(ctx context.Context, in VerifyOTPInput) (string, error) {
	if f.verify != nil {
		return f.verify(ctx, in)
	}
	return "", apperror.ErrUnauthorized("invalid OTP")
}

func validRegistrationMaterial() RegisterInput {
	return RegisterInput{
		KdfSalt:             encode(make([]byte, 16)),
		PasswordKeyEnvelope: make([]byte, 48),
		PasswordKeyNonce:    make([]byte, 12),
		RecoveryKeyHash:     make([]byte, 32),
		RecoveryKeyEnvelope: make([]byte, 48),
		RecoveryKeyNonce:    make([]byte, 12),
	}
}

func testRegisterOTPService() OTPService {
	return fakeOTPService{verify: func(_ context.Context, in VerifyOTPInput) (string, error) {
		if in.Purpose != entity.OTPPurposeRegister {
			return "", apperror.ErrUnauthorized("invalid OTP purpose")
		}
		return hashIdentifier("+989121234567", testSecret), nil
	}}
}

func newService(users *mocks.MockUserRepository, refresh *mocks.MockRefreshTokenRepository) *ServiceImpl {
	return NewService(users, refresh, testSecret, time.Minute, 24*time.Hour)
}

func TestRegisterSuccess(t *testing.T) {
	var created *entity.User
	users := &mocks.MockUserRepository{
		FindByPhoneHashFunc: func(_ context.Context, _ string) (*entity.User, error) { return nil, apperror.ErrNotFound("not found") },
		CreateFunc: func(_ context.Context, u *entity.User) error {
			u.ID = "507f1f77bcf86cd799439011"
			created = u
			return nil
		},
	}
	refresh := &mocks.MockRefreshTokenRepository{CreateFunc: func(_ context.Context, r *entity.RefreshToken) error {
		if r.TokenHash == "" {
			t.Fatal("refresh token hash is empty")
		}
		return nil
	}}
	s := NewService(users, refresh, testSecret, time.Minute, 24*time.Hour, testRegisterOTPService())
	in := validRegistrationMaterial()
	in.PhoneNumber, in.AuthSecret, in.DeviceID, in.OTPChallengeID, in.OTPCode = "+989121234567", testAuthSecret, "device-1", "challenge-1", "123456"
	out, err := s.Register(context.Background(), in)
	if err != nil {
		t.Fatal(err)
	}
	if created == nil || created.PasswordHash == "" || created.PhoneHash == "+989121234567" {
		t.Fatal("credentials were not stored securely")
	}
	if out.RefreshToken == "" || out.AccessToken == "" || out.KdfSalt == "" {
		t.Fatal("session output incomplete")
	}
}

func TestRegisterDuplicate(t *testing.T) {
	users := &mocks.MockUserRepository{FindByPhoneHashFunc: func(context.Context, string) (*entity.User, error) { return &entity.User{ID: "u1"}, nil }}
	s := NewService(users, &mocks.MockRefreshTokenRepository{}, testSecret, time.Minute, 24*time.Hour, testRegisterOTPService())
	in := validRegistrationMaterial()
	in.PhoneNumber, in.AuthSecret, in.DeviceID, in.OTPChallengeID, in.OTPCode = "+989121234567", testAuthSecret, "d", "challenge-1", "123456"
	_, err := s.Register(context.Background(), in)
	code, _ := apperror.CodeOf(err)
	if code != apperror.CodeConflict {
		t.Fatalf("got %v", err)
	}
}

func TestLogoutRevokesRefreshAndAccessSessions(t *testing.T) {
	raw := "logout-refresh-token"
	versionIncremented := false
	users := &mocks.MockUserRepository{IncrementSessionVersionFunc: func(context.Context, string) (uint64, error) { versionIncremented = true; return 2, nil }}
	refresh := &mocks.MockRefreshTokenRepository{FindByHashFunc: func(context.Context, string) (*entity.RefreshToken, error) {
		return &entity.RefreshToken{UserID: "u1", TokenHash: infraauth.HashRefreshToken(raw)}, nil
	}}
	s := newService(users, refresh)
	if err := s.Logout(context.Background(), raw); err != nil || !versionIncremented {
		t.Fatalf("err=%v versionIncremented=%v", err, versionIncremented)
	}
}

func TestLoginSuccessAndWrongPassword(t *testing.T) {
	salt, _ := infraauth.GenerateSalt()
	hash, _ := infraauth.HashPassword(testAuthSecret, salt)
	user := &entity.User{ID: "u1", PasswordHash: hash, AuthSalt: encode(salt), KdfSalt: encode(make([]byte, 16))}
	users := &mocks.MockUserRepository{FindByPhoneHashFunc: func(context.Context, string) (*entity.User, error) { return user, nil }, AddDeviceFunc: func(context.Context, string, string) error { return nil }}
	refresh := &mocks.MockRefreshTokenRepository{}
	s := newService(users, refresh)
	if _, err := s.Login(context.Background(), LoginInput{Identifier: "+989121234567", AuthSecret: testAuthSecret, DeviceID: "d"}); err != nil {
		t.Fatal(err)
	}
	if _, err := s.Login(context.Background(), LoginInput{Identifier: "+989121234567", AuthSecret: testWrongSecret, DeviceID: "d"}); func() bool { code, _ := apperror.CodeOf(err); return code != apperror.CodeUnauthorized }() {
		t.Fatalf("got %v", err)
	}
}

// signInFixture builds a service whose user can log in with the given password and which
// reports `known` as the devices already registered on the account.
func signInFixture(known map[string]bool, events *mocks.MockDeviceEventRepository) *ServiceImpl {
	salt, _ := infraauth.GenerateSalt()
	hash, _ := infraauth.HashPassword(testAuthSecret, salt)
	user := &entity.User{ID: "u1", PasswordHash: hash, AuthSalt: encode(salt), KdfSalt: encode(make([]byte, 16))}
	users := &mocks.MockUserRepository{
		FindByPhoneHashFunc: func(context.Context, string) (*entity.User, error) { return user, nil },
		AddDeviceFunc:       func(context.Context, string, string) error { return nil },
	}
	devices := &mocks.MockDeviceRepository{ExistsFunc: func(_ context.Context, _, d string) (bool, error) { return known[d], nil }}
	s := newService(users, &mocks.MockRefreshTokenRepository{})
	s.SetDeviceRepository(devices)
	s.SetDeviceEventRepository(events)
	return s
}

func TestLoginFromANewDeviceRecordsSignInEvent(t *testing.T) {
	var recorded []*entity.DeviceEvent
	events := &mocks.MockDeviceEventRepository{RecordFunc: func(_ context.Context, e *entity.DeviceEvent) error {
		recorded = append(recorded, e)
		return nil
	}}
	s := signInFixture(map[string]bool{"existing": true}, events)

	// A device that is already part of the account signing in again is not news.
	if _, err := s.Login(context.Background(), LoginInput{Identifier: "+989121234567", AuthSecret: testAuthSecret, DeviceID: "existing"}); err != nil {
		t.Fatal(err)
	}
	if len(recorded) != 0 {
		t.Fatalf("known device recorded %d events, want 0", len(recorded))
	}

	// A device that was not part of the account is.
	in := LoginInput{Identifier: "+989121234567", AuthSecret: testAuthSecret, DeviceID: "new-phone", Device: DeviceInfo{Name: "Pixel 8", Platform: "android"}}
	if _, err := s.Login(context.Background(), in); err != nil {
		t.Fatal(err)
	}
	if len(recorded) != 1 {
		t.Fatalf("new device recorded %d events, want 1", len(recorded))
	}
	got := recorded[0]
	if got.Type != entity.DeviceEventSignedIn || got.UserID != "u1" || got.DeviceID != "new-phone" ||
		got.DeviceName != "Pixel 8" || got.Platform != "android" || got.ActorDeviceID != "new-phone" {
		t.Fatalf("unexpected event: %+v", got)
	}
}

func TestLoginSucceedsEvenWhenTheSignInEventCannotBeRecorded(t *testing.T) {
	events := &mocks.MockDeviceEventRepository{RecordFunc: func(context.Context, *entity.DeviceEvent) error {
		return apperror.ErrInternal("event store down")
	}}
	s := signInFixture(nil, events)
	if _, err := s.Login(context.Background(), LoginInput{Identifier: "+989121234567", AuthSecret: testAuthSecret, DeviceID: "new-phone"}); err != nil {
		t.Fatalf("login must not depend on the event feed: %v", err)
	}
}

func TestRefreshWithTokenOfRemovedDeviceDoesNotSignOutTheAccount(t *testing.T) {
	raw := "removed-device-token"
	revokedAt := time.Now().Add(-time.Minute)
	token := &entity.RefreshToken{TokenHash: infraauth.HashRefreshToken(raw), UserID: "u1", DeviceID: "gone", RevokedAt: &revokedAt, ExpiresAt: time.Now().Add(time.Hour)}
	allRevoked, versionBumped := false, false
	refresh := &mocks.MockRefreshTokenRepository{
		FindByHashFunc:       func(context.Context, string) (*entity.RefreshToken, error) { return token, nil },
		RevokeAllForUserFunc: func(context.Context, string) error { allRevoked = true; return nil },
	}
	users := &mocks.MockUserRepository{IncrementSessionVersionFunc: func(context.Context, string) (uint64, error) { versionBumped = true; return 1, nil }}
	s := newService(users, refresh)
	s.SetDeviceRepository(&mocks.MockDeviceRepository{ExistsFunc: func(context.Context, string, string) (bool, error) { return false, nil }})

	_, err := s.Refresh(context.Background(), RefreshInput{RefreshToken: raw})
	if code, ok := apperror.CodeOf(err); !ok || code != apperror.CodeDeviceRemoved {
		t.Fatalf("err = %v, want DEVICE_REMOVED", err)
	}
	if allRevoked || versionBumped {
		t.Fatalf("a removed device signed out the whole account: revokeAll=%v versionBumped=%v", allRevoked, versionBumped)
	}
}

func TestRefreshReuseOfRotatedTokenStillSignsOutTheAccountWhenDeviceExists(t *testing.T) {
	raw := "rotated-token"
	revokedAt := time.Now().Add(-time.Minute)
	token := &entity.RefreshToken{TokenHash: infraauth.HashRefreshToken(raw), UserID: "u1", DeviceID: "d1", RevokedAt: &revokedAt, ExpiresAt: time.Now().Add(time.Hour)}
	allRevoked := false
	refresh := &mocks.MockRefreshTokenRepository{
		FindByHashFunc:       func(context.Context, string) (*entity.RefreshToken, error) { return token, nil },
		RevokeAllForUserFunc: func(context.Context, string) error { allRevoked = true; return nil },
	}
	s := newService(&mocks.MockUserRepository{}, refresh)
	s.SetDeviceRepository(&mocks.MockDeviceRepository{ExistsFunc: func(context.Context, string, string) (bool, error) { return true, nil }})

	_, err := s.Refresh(context.Background(), RefreshInput{RefreshToken: raw})
	if code, ok := apperror.CodeOf(err); !ok || code != apperror.CodeUnauthorized {
		t.Fatalf("err = %v, want UNAUTHORIZED", err)
	}
	if !allRevoked {
		t.Fatal("genuine token reuse must still revoke every session")
	}
}

func encode(b []byte) string { return base64.RawStdEncoding.EncodeToString(b) }

func TestRefreshSuccess(t *testing.T) {
	raw := "old-refresh-token"
	old := &entity.RefreshToken{TokenHash: infraauth.HashRefreshToken(raw), UserID: "u1", DeviceID: "d1", CreatedAt: time.Now().Add(-time.Hour), ExpiresAt: time.Now().Add(time.Hour)}
	users := &mocks.MockUserRepository{FindByIDFunc: func(context.Context, string) (*entity.User, error) {
		return &entity.User{ID: "u1", KdfSalt: encode(make([]byte, 16))}, nil
	}}
	var revoked bool
	refresh := &mocks.MockRefreshTokenRepository{
		FindByHashFunc: func(context.Context, string) (*entity.RefreshToken, error) { return old, nil },
		RevokeFunc:     func(context.Context, string) error { revoked = true; return nil },
		CreateFunc: func(_ context.Context, r *entity.RefreshToken) error {
			if r.TokenHash == raw {
				t.Fatal("raw refresh token stored")
			}
			return nil
		},
	}
	out, err := newService(users, refresh).Refresh(context.Background(), RefreshInput{RefreshToken: raw})
	if err != nil || !revoked || out.RefreshToken == "" || out.AccessToken == "" {
		t.Fatalf("out=%+v revoked=%v err=%v", out, revoked, err)
	}
	if out.RefreshToken == raw {
		t.Fatal("refresh token was not rotated")
	}
}

func TestRefreshExpired(t *testing.T) {
	raw := "expired-refresh-token"
	refresh := &mocks.MockRefreshTokenRepository{FindByHashFunc: func(context.Context, string) (*entity.RefreshToken, error) {
		return &entity.RefreshToken{TokenHash: infraauth.HashRefreshToken(raw), UserID: "u1", ExpiresAt: time.Now().Add(-time.Second)}, nil
	}}
	_, err := newService(&mocks.MockUserRepository{}, refresh).Refresh(context.Background(), RefreshInput{RefreshToken: raw})
	code, _ := apperror.CodeOf(err)
	if code != apperror.CodeUnauthorized {
		t.Fatalf("got %v", err)
	}
}

func TestRefreshRevoked(t *testing.T) {
	raw := "revoked-refresh-token"
	revokedAll := false
	refresh := &mocks.MockRefreshTokenRepository{
		FindByHashFunc: func(context.Context, string) (*entity.RefreshToken, error) {
			now := time.Now()
			return &entity.RefreshToken{TokenHash: infraauth.HashRefreshToken(raw), UserID: "u1", ExpiresAt: now.Add(time.Hour), RevokedAt: &now}, nil
		},
		RevokeAllForUserFunc: func(context.Context, string) error { revokedAll = true; return nil },
	}
	_, err := newService(&mocks.MockUserRepository{}, refresh).Refresh(context.Background(), RefreshInput{RefreshToken: raw})
	code, _ := apperror.CodeOf(err)
	if code != apperror.CodeUnauthorized || !revokedAll {
		t.Fatalf("err=%v revokedAll=%v", err, revokedAll)
	}
}

func TestRefreshReuseRevokesAllSessions(t *testing.T) {
	raw := "reused-refresh-token"
	revokedAll := false
	refresh := &mocks.MockRefreshTokenRepository{
		FindByHashFunc: func(context.Context, string) (*entity.RefreshToken, error) {
			return &entity.RefreshToken{TokenHash: infraauth.HashRefreshToken(raw), UserID: "u1", DeviceID: "d1", ExpiresAt: time.Now().Add(time.Hour)}, nil
		},
		RevokeFunc:           func(context.Context, string) error { return apperror.ErrConflict("already revoked") },
		RevokeAllForUserFunc: func(context.Context, string) error { revokedAll = true; return nil },
	}
	_, err := newService(&mocks.MockUserRepository{FindByIDFunc: func(context.Context, string) (*entity.User, error) { return &entity.User{ID: "u1"}, nil }}, refresh).Refresh(context.Background(), RefreshInput{RefreshToken: raw})
	code, _ := apperror.CodeOf(err)
	if code != apperror.CodeUnauthorized || !revokedAll {
		t.Fatalf("err=%v revokedAll=%v", err, revokedAll)
	}
}

func TestLoginLocksAfterThreshold(t *testing.T) {
	salt, _ := infraauth.GenerateSalt()
	hash, _ := infraauth.HashPassword(testAuthSecret, salt)
	user := &entity.User{ID: "u1", PasswordHash: hash, AuthSalt: encode(salt)}
	attempts := 0
	users := &mocks.MockUserRepository{
		FindByPhoneHashFunc:   func(context.Context, string) (*entity.User, error) { return user, nil },
		RecordFailedLoginFunc: func(context.Context, string, time.Time, int, time.Duration) error { attempts++; return nil },
	}
	s := newService(users, &mocks.MockRefreshTokenRepository{})
	for i := 0; i < 5; i++ {
		_, _ = s.Login(context.Background(), LoginInput{Identifier: "+989121234567", AuthSecret: testWrongSecret, DeviceID: "d"})
	}
	if attempts != 5 {
		t.Fatalf("attempts=%d", attempts)
	}
}

func TestLoginSkipsPasswordVerificationWhenLocked(t *testing.T) {
	future := time.Now().Add(time.Minute)
	users := &mocks.MockUserRepository{FindByPhoneHashFunc: func(context.Context, string) (*entity.User, error) {
		return &entity.User{ID: "u1", AuthSalt: "invalid", PasswordHash: "invalid", LockedUntil: &future}, nil
	}}
	s := newService(users, &mocks.MockRefreshTokenRepository{})
	_, err := s.Login(context.Background(), LoginInput{Identifier: "+989121234567", AuthSecret: testWrongSecret, DeviceID: "d"})
	code, _ := apperror.CodeOf(err)
	if code != apperror.CodeUnauthorized {
		t.Fatalf("code=%v err=%v", code, err)
	}
}

func TestLoginSuccessResetsFailedAttempts(t *testing.T) {
	salt, _ := infraauth.GenerateSalt()
	hash, _ := infraauth.HashPassword(testAuthSecret, salt)
	user := &entity.User{ID: "u1", PasswordHash: hash, AuthSalt: encode(salt), FailedLoginAttempts: 4}
	reset := false
	users := &mocks.MockUserRepository{
		FindByPhoneHashFunc:  func(context.Context, string) (*entity.User, error) { return user, nil },
		ResetFailedLoginFunc: func(context.Context, string) error { reset = true; return nil },
		AddDeviceFunc:        func(context.Context, string, string) error { return nil },
	}
	_, err := newService(users, &mocks.MockRefreshTokenRepository{}).Login(context.Background(), LoginInput{Identifier: "+989121234567", AuthSecret: testAuthSecret, DeviceID: "d"})
	if err != nil || !reset {
		t.Fatalf("err=%v reset=%v", err, reset)
	}
}

func TestLoginAllowsVerificationAfterLockExpiry(t *testing.T) {
	past := time.Now().Add(-time.Minute)
	salt, _ := infraauth.GenerateSalt()
	hash, _ := infraauth.HashPassword(testAuthSecret, salt)
	user := &entity.User{ID: "u1", PasswordHash: hash, AuthSalt: encode(salt), LockedUntil: &past}
	reset := false
	users := &mocks.MockUserRepository{
		FindByPhoneHashFunc:  func(context.Context, string) (*entity.User, error) { return user, nil },
		ResetFailedLoginFunc: func(context.Context, string) error { reset = true; return nil },
		AddDeviceFunc:        func(context.Context, string, string) error { return nil },
	}
	if _, err := newService(users, &mocks.MockRefreshTokenRepository{}).Login(context.Background(), LoginInput{Identifier: "+989121234567", AuthSecret: testAuthSecret, DeviceID: "d"}); err != nil || !reset {
		t.Fatalf("err=%v reset=%v", err, reset)
	}
}

func TestRecoveryProofVerification(t *testing.T) {
	recovery := []byte("01234567890123456789012345678901")
	proof := infraauth.RecoveryProofFromBytes(recovery)
	proofBytes, err := base64.StdEncoding.DecodeString(proof)
	if err != nil {
		t.Fatal(err)
	}
	stored := base64.RawURLEncoding.EncodeToString(infraauth.RecoveryKeyHashFromProof(proofBytes))

	if !verifyRecoveryProof(stored, proof) {
		t.Fatal("the proof derived from the recovery key must verify")
	}
	if !verifyRecoveryProof(stored, base64.RawStdEncoding.EncodeToString(proofBytes)) {
		t.Fatal("padding of the proof must not matter")
	}
	// The recovery key itself is not a valid proof: sending it must never succeed.
	if verifyRecoveryProof(stored, base64.StdEncoding.EncodeToString(recovery)) {
		t.Fatal("the raw recovery key must not be accepted as a proof")
	}
	if verifyRecoveryProof("", proof) || verifyRecoveryProof(stored, "short") {
		t.Fatal("empty or malformed values must not verify")
	}
}

func TestAuthSecretRejectsAnythingThatIsNotAKeyDerivedValue(t *testing.T) {
	if _, err := canonicalAuthSecret("correct horse battery staple"); err == nil {
		t.Fatal("a raw password must be rejected")
	}
	if _, err := canonicalAuthSecret(""); err == nil {
		t.Fatal("empty secret must be rejected")
	}
	got, err := canonicalAuthSecret(testAuthSecret)
	if err != nil || got != testAuthSecret {
		t.Fatalf("valid secret rejected or altered: %v %q", err, got)
	}
	unpadded := testAuthSecret[:len(testAuthSecret)-1]
	if got, err := canonicalAuthSecret(unpadded); err != nil || got != testAuthSecret {
		t.Fatalf("unpadded spelling must canonicalise: %v %q", err, got)
	}
}

func TestLoginRejectsRawPasswordsBeforeAnyLookup(t *testing.T) {
	users := &mocks.MockUserRepository{}
	_, err := newService(users, &mocks.MockRefreshTokenRepository{}).Login(context.Background(), LoginInput{Identifier: "+989121234567", AuthSecret: "correct horse battery", DeviceID: "d"})
	requireCode(t, err, apperror.CodeValidation)
}

func TestLoginParamsDecoySaltIsDeterministicAndIndistinguishable(t *testing.T) {
	salt := make([]byte, 16)
	for i := range salt {
		salt[i] = byte(i + 1)
	}
	known := &entity.User{ID: "u1", KdfSalt: base64.RawStdEncoding.EncodeToString(salt)}
	users := &mocks.MockUserRepository{FindByPhoneHashFunc: func(_ context.Context, hash string) (*entity.User, error) {
		if hash == hashIdentifier("+989121234567", testSecret) {
			return known, nil
		}
		return nil, apperror.ErrNotFound("user")
	}}
	s := NewService(users, &mocks.MockRefreshTokenRepository{}, testSecret, time.Minute, 24*time.Hour, testRegisterOTPService())

	real, err := s.LoginParams(context.Background(), "+989121234567")
	if err != nil || real.KdfSalt != base64.StdEncoding.EncodeToString(salt) || real.KdfVersion != infraauth.KDFVersion || real.KdfIterations != infraauth.ClientPBKDF2Iterations {
		t.Fatalf("real params wrong: %+v %v", real, err)
	}
	a, _ := s.LoginParams(context.Background(), "+989129999999")
	b, _ := s.LoginParams(context.Background(), "+989129999999")
	if a.KdfSalt == "" || a.KdfSalt != b.KdfSalt {
		t.Fatal("decoy salt must be deterministic")
	}
	if raw, err := base64.StdEncoding.DecodeString(a.KdfSalt); err != nil || len(raw) != 16 {
		t.Fatal("decoy salt must look exactly like a real one")
	}
}

func mustDecodeRawURL(t *testing.T, value string) []byte {
	t.Helper()
	decoded, err := base64.RawURLEncoding.DecodeString(value)
	if err != nil {
		t.Fatal(err)
	}
	return decoded
}
