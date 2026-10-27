package auth

import (
	"context"
	"testing"
	"time"

	"github.com/mosinaRZ/finance-sync-backend/internal/domain/entity"
	"github.com/mosinaRZ/finance-sync-backend/internal/usecase/mocks"
	"github.com/stretchr/testify/require"
)

func TestKeysValidate(t *testing.T) {
	require.NoError(t, testKeys.Validate())

	for name, mutate := range map[string]func(*Keys){
		"missing jwt":      func(k *Keys) { k.JWTSecret = "" },
		"missing phone":    func(k *Keys) { k.PhoneHashKey = " " },
		"missing otp":      func(k *Keys) { k.OTPCodeKey = "" },
		"missing decoy":    func(k *Keys) { k.DecoySaltKey = "" },
		"short key":        func(k *Keys) { k.OTPCodeKey = "short" },
		"phone eq jwt":     func(k *Keys) { k.PhoneHashKey = k.JWTSecret },
		"otp eq phone":     func(k *Keys) { k.OTPCodeKey = k.PhoneHashKey },
		"decoy eq jwt":     func(k *Keys) { k.DecoySaltKey = k.JWTSecret },
		"decoy eq otp key": func(k *Keys) { k.DecoySaltKey = k.OTPCodeKey },
	} {
		k := testKeys
		mutate(&k)
		require.Error(t, k.Validate(), name)
	}
}

// Rotating the JWT secret must not change what identifies an account.
func TestPhoneHashIsIndependentOfJWTSecret(t *testing.T) {
	rotated := testKeys
	rotated.JWTSecret = "rotated-jwt-secret-0123456789abcdef012345"

	var seen []string
	users := &mocks.MockUserRepository{
		FindByPhoneHashFunc: func(_ context.Context, h string) (*entity.User, error) {
			seen = append(seen, h)
			return nil, nil
		},
	}
	for _, k := range []Keys{testKeys, rotated} {
		svc := NewService(users, &mocks.MockRefreshTokenRepository{}, k, time.Minute, time.Hour)
		_, err := svc.LoginParams(context.Background(), "+989121234567")
		require.NoError(t, err)
	}
	require.Len(t, seen, 2)
	require.Equal(t, seen[0], seen[1], "phone identifier hash must not depend on the JWT secret")
	require.Equal(t, hashIdentifier("+989121234567", testKeys.PhoneHashKey), seen[0])
	require.NotEqual(t, hashIdentifier("+989121234567", testKeys.JWTSecret), seen[0],
		"the JWT secret must not be used as the phone hash key")
}

func TestPhoneHashDependsOnPhoneHashKey(t *testing.T) {
	a := hashIdentifier("+989121234567", testKeys.PhoneHashKey)
	b := hashIdentifier("+989121234567", "another-phone-hash-key-0123456789abcdef01")
	require.NotEqual(t, a, b)
}

// The decoy salt for unknown identifiers is keyed by its own key only.
func TestDecoyKdfSaltUsesOwnKey(t *testing.T) {
	users := &mocks.MockUserRepository{
		FindByPhoneHashFunc: func(context.Context, string) (*entity.User, error) { return nil, nil },
	}
	salt := func(k Keys) string {
		out, err := NewService(users, &mocks.MockRefreshTokenRepository{}, k, time.Minute, time.Hour).
			LoginParams(context.Background(), "+989129999999")
		require.NoError(t, err)
		return out.KdfSalt
	}
	base := salt(testKeys)
	require.Equal(t, base, salt(testKeys), "decoy salt must be deterministic")

	otherJWT, otherPhone, otherOTP := testKeys, testKeys, testKeys
	otherJWT.JWTSecret = "rotated-jwt-secret-0123456789abcdef012345"
	otherPhone.PhoneHashKey = "rotated-phone-key-0123456789abcdef012345"
	otherOTP.OTPCodeKey = "rotated-otp-key-0123456789abcdef01234567"
	require.Equal(t, base, salt(otherJWT), "rotating the JWT secret must not change decoy salts")
	require.Equal(t, base, salt(otherPhone), "rotating the phone key must not change decoy salts")
	require.Equal(t, base, salt(otherOTP), "rotating the OTP key must not change decoy salts")

	otherDecoy := testKeys
	otherDecoy.DecoySaltKey = "rotated-decoy-key-0123456789abcdef01234"
	require.NotEqual(t, base, salt(otherDecoy))
}

// The OTP code hash is keyed by the OTP key only: the other keys are irrelevant to it.
func TestOTPCodeHashUsesOwnKey(t *testing.T) {
	h := hashOTP("id", "123456", testKeys.OTPCodeKey)
	require.Equal(t, h, hashOTP("id", "123456", testKeys.OTPCodeKey))
	require.NotEqual(t, h, hashOTP("id", "123456", testKeys.JWTSecret))
	require.NotEqual(t, h, hashOTP("id", "123456", testKeys.PhoneHashKey))
	require.NotEqual(t, h, hashOTP("id2", "123456", testKeys.OTPCodeKey), "bound to the challenge id")
	require.NotEqual(t, h, hashOTP("id", "654321", testKeys.OTPCodeKey))
}

func TestOTPServiceRefusesToRunWithoutItsKeys(t *testing.T) {
	for name, mutate := range map[string]func(*Keys){
		"no phone key": func(k *Keys) { k.PhoneHashKey = "" },
		"no otp key":   func(k *Keys) { k.OTPCodeKey = "" },
	} {
		k := testKeys
		mutate(&k)
		s := NewOTPService(&otpRepoMock{}, &deliveryMock{}, k, time.Minute)
		_, err := s.Request(context.Background(), RequestOTPInput{Destination: "+989121234567", Channel: "sms", Purpose: entity.OTPPurposeLogin})
		require.Error(t, err, name)
	}
}

// A challenge issued by the OTP service must be bound to the same destination hash that the
// account service computes, otherwise registration could never succeed.
func TestOTPDestinationHashMatchesAccountPhoneHash(t *testing.T) {
	repo := &otpRepoMock{}
	d := &deliveryMock{}
	s := NewOTPService(repo, d, testKeys, 5*time.Minute)
	out, err := s.Request(context.Background(), RequestOTPInput{Destination: "+989121234567", Channel: "sms", Purpose: entity.OTPPurposeRegister})
	require.NoError(t, err)
	got, err := s.Verify(context.Background(), VerifyOTPInput{ChallengeID: out.ChallengeID, Code: d.code, Purpose: entity.OTPPurposeRegister})
	require.NoError(t, err)
	require.Equal(t, hashIdentifier("+989121234567", testKeys.PhoneHashKey), got)
}
