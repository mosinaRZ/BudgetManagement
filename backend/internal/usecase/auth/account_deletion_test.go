package auth

import (
	"context"
	"testing"
	"time"

	"github.com/mosinaRZ/finance-sync-backend/internal/domain/apperror"
	"github.com/mosinaRZ/finance-sync-backend/internal/domain/entity"
	infraauth "github.com/mosinaRZ/finance-sync-backend/internal/infrastructure/auth"
	"github.com/mosinaRZ/finance-sync-backend/internal/usecase/mocks"
)

const deletionTestPassword = "correct horse battery"

// deletionFixture is an account that can sign in with deletionTestPassword.
func deletionFixture(t *testing.T, deletedAt *time.Time) *entity.User {
	t.Helper()
	salt, err := infraauth.GenerateSalt()
	if err != nil {
		t.Fatal(err)
	}
	hash, err := infraauth.HashPassword(deletionTestPassword, salt)
	if err != nil {
		t.Fatal(err)
	}
	return &entity.User{
		ID:           "u1",
		Role:         entity.RoleUser,
		PasswordHash: hash,
		AuthSalt:     encode(salt),
		KdfSalt:      encode(make([]byte, 16)),
		IsDeleted:    deletedAt != nil,
		DeletedAt:    deletedAt,
	}
}

func requireCode(t *testing.T, err error, want apperror.Code) {
	t.Helper()
	if code, ok := apperror.CodeOf(err); !ok || code != want {
		t.Fatalf("err = %v, want code %s", err, want)
	}
}

func TestDeleteAccountSoftDeletesAndRevokesEverySession(t *testing.T) {
	user := deletionFixture(t, nil)
	var deletedFlag *bool
	revokedAll := false
	users := &mocks.MockUserRepository{
		FindByIDFunc: func(context.Context, string) (*entity.User, error) { return user, nil },
		SetDeletedFunc: func(_ context.Context, id string, deleted bool, _ time.Time) (uint64, error) {
			if id != "u1" {
				t.Fatalf("deleted wrong user %q", id)
			}
			deletedFlag = &deleted
			return 2, nil
		},
	}
	refresh := &mocks.MockRefreshTokenRepository{RevokeAllForUserFunc: func(context.Context, string) error { revokedAll = true; return nil }}
	s := newService(users, refresh)

	before := time.Now().UTC()
	out, err := s.DeleteAccount(context.Background(), DeleteAccountInput{UserID: "u1", Password: deletionTestPassword})
	if err != nil {
		t.Fatal(err)
	}
	if deletedFlag == nil || !*deletedFlag {
		t.Fatal("account was not flagged isDeleted=true")
	}
	if !revokedAll {
		t.Fatal("refresh tokens were not revoked")
	}
	if out.DeletedAt.Before(before) {
		t.Fatalf("DeletedAt = %v, want >= %v", out.DeletedAt, before)
	}
	if got := out.RestorableUntil.Sub(out.DeletedAt); got != DefaultAccountRestoreWindow {
		t.Fatalf("restore window = %v, want %v", got, DefaultAccountRestoreWindow)
	}
}

func TestDeleteAccountRequiresTheCorrectPassword(t *testing.T) {
	user := deletionFixture(t, nil)
	failed, deleted := false, false
	users := &mocks.MockUserRepository{
		FindByIDFunc:          func(context.Context, string) (*entity.User, error) { return user, nil },
		RecordFailedLoginFunc: func(context.Context, string, time.Time, int, time.Duration) error { failed = true; return nil },
		SetDeletedFunc:        func(context.Context, string, bool, time.Time) (uint64, error) { deleted = true; return 1, nil },
	}
	s := newService(users, &mocks.MockRefreshTokenRepository{})

	_, err := s.DeleteAccount(context.Background(), DeleteAccountInput{UserID: "u1", Password: "wrong password!"})
	requireCode(t, err, apperror.CodeInvalidPassword)
	if !failed {
		t.Fatal("a wrong password must count towards the lockout")
	}
	if deleted {
		t.Fatal("account deleted despite a wrong password")
	}

	_, err = s.DeleteAccount(context.Background(), DeleteAccountInput{UserID: "u1", Password: ""})
	requireCode(t, err, apperror.CodeValidation)
}

func TestDeleteAccountOfAnAlreadyDeletedAccountIsRejected(t *testing.T) {
	at := time.Now().Add(-time.Hour)
	user := deletionFixture(t, &at)
	users := &mocks.MockUserRepository{FindByIDFunc: func(context.Context, string) (*entity.User, error) { return user, nil }}
	_, err := newService(users, &mocks.MockRefreshTokenRepository{}).DeleteAccount(context.Background(), DeleteAccountInput{UserID: "u1", Password: deletionTestPassword})
	requireCode(t, err, apperror.CodeUnauthorized)
}

func loginUsers(user *entity.User, setDeleted func(bool)) *mocks.MockUserRepository {
	return &mocks.MockUserRepository{
		FindByPhoneHashFunc: func(context.Context, string) (*entity.User, error) { return user, nil },
		AddDeviceFunc:       func(context.Context, string, string) error { return nil },
		SetDeletedFunc: func(_ context.Context, _ string, deleted bool, _ time.Time) (uint64, error) {
			if setDeleted != nil {
				setDeleted(deleted)
			}
			return 7, nil
		},
	}
}

func TestLoginOfADeletedAccountReportsItOnlyAfterTheCorrectPassword(t *testing.T) {
	at := time.Now().Add(-time.Hour)
	user := deletionFixture(t, &at)
	s := newService(loginUsers(user, nil), &mocks.MockRefreshTokenRepository{})

	// Correct password, no restore requested: the client learns the account is deleted.
	_, err := s.Login(context.Background(), LoginInput{Identifier: "+989121234567", Password: deletionTestPassword, DeviceID: "d"})
	requireCode(t, err, apperror.CodeAccountDeleted)

	// Wrong password: indistinguishable from any other failed sign-in (no account probing).
	_, err = s.Login(context.Background(), LoginInput{Identifier: "+989121234567", Password: "wrong password!", DeviceID: "d", RestoreAccount: true})
	requireCode(t, err, apperror.CodeUnauthorized)
}

func TestLoginWithRestoreReactivatesTheAccountWithinTheGracePeriod(t *testing.T) {
	at := time.Now().Add(-24 * time.Hour)
	user := deletionFixture(t, &at)
	var restored *bool
	s := newService(loginUsers(user, func(d bool) { restored = &d }), &mocks.MockRefreshTokenRepository{})

	out, err := s.Login(context.Background(), LoginInput{Identifier: "+989121234567", Password: deletionTestPassword, DeviceID: "d", RestoreAccount: true})
	if err != nil {
		t.Fatal(err)
	}
	if restored == nil || *restored {
		t.Fatal("account was not restored (isDeleted=false)")
	}
	if out.AccessToken == "" || out.RefreshToken == "" {
		t.Fatal("a restored account must receive a session")
	}
	claims, err := infraauth.ParseAndValidateAccessTokenClaims(out.AccessToken, testSecret)
	if err != nil {
		t.Fatal(err)
	}
	if claims.SessionVersion != 7 {
		t.Fatalf("token session version = %d, want the post-restore version 7", claims.SessionVersion)
	}
}

func TestLoginAfterTheGracePeriodTreatsTheAccountAsGone(t *testing.T) {
	at := time.Now().Add(-DefaultAccountRestoreWindow - time.Hour)
	user := deletionFixture(t, &at)
	restored := false
	s := newService(loginUsers(user, func(bool) { restored = true }), &mocks.MockRefreshTokenRepository{})

	_, err := s.Login(context.Background(), LoginInput{Identifier: "+989121234567", Password: deletionTestPassword, DeviceID: "d", RestoreAccount: true})
	requireCode(t, err, apperror.CodeUnauthorized)
	if restored {
		t.Fatal("an expired account must not be restorable")
	}
}

func TestRefreshRejectsADeletedAccount(t *testing.T) {
	raw := "refresh-of-deleted-account"
	at := time.Now().Add(-time.Minute)
	user := deletionFixture(t, &at)
	token := &entity.RefreshToken{TokenHash: infraauth.HashRefreshToken(raw), UserID: "u1", DeviceID: "d1", ExpiresAt: time.Now().Add(time.Hour)}
	revoked := false
	users := &mocks.MockUserRepository{FindByIDFunc: func(context.Context, string) (*entity.User, error) { return user, nil }}
	refresh := &mocks.MockRefreshTokenRepository{
		FindByHashFunc: func(context.Context, string) (*entity.RefreshToken, error) { return token, nil },
		RevokeFunc:     func(context.Context, string) error { revoked = true; return nil },
	}
	_, err := newService(users, refresh).Refresh(context.Background(), RefreshInput{RefreshToken: raw})
	requireCode(t, err, apperror.CodeUnauthorized)
	if revoked {
		t.Fatal("a deleted account must not rotate refresh tokens")
	}
}

func TestRegisterWithThePhoneOfADeletedAccountPointsToRestore(t *testing.T) {
	at := time.Now().Add(-time.Hour)
	user := deletionFixture(t, &at)
	users := &mocks.MockUserRepository{FindByPhoneHashFunc: func(context.Context, string) (*entity.User, error) { return user, nil }}
	s := NewService(users, &mocks.MockRefreshTokenRepository{}, testSecret, time.Minute, 24*time.Hour, testRegisterOTPService())
	in := validRegistrationMaterial()
	in.PhoneNumber, in.Password, in.DeviceID, in.OTPChallengeID, in.OTPCode = "+989121234567", "correct horse battery", "d", "challenge-1", "123456"
	_, err := s.Register(context.Background(), in)
	requireCode(t, err, apperror.CodeConflict)
}
