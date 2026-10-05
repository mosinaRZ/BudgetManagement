package auth

import (
	"context"
	"testing"
	"time"

	"github.com/mosinaRZ/finance-sync-backend/internal/domain/entity"
	"github.com/mosinaRZ/finance-sync-backend/internal/usecase/mocks"
)

func TestGetProfileReturnsAccountProfile(t *testing.T) {
	user := &entity.User{
		ID: "u1", PhoneNumber: "+989121234567", Email: "a@example.com",
		EmailVerified: true, FirstName: "Ali", LastName: "Ahmadi",
		Gender: entity.GenderMale, BirthDate: "2000-01-02",
	}
	svc := NewService(&mocks.MockUserRepository{
		FindByIDFunc: func(context.Context, string) (*entity.User, error) { return user, nil },
	}, &mocks.MockRefreshTokenRepository{}, testSecret, time.Minute, time.Hour)

	got, err := svc.GetProfile(context.Background(), "u1")
	if err != nil {
		t.Fatal(err)
	}
	if got.Email != user.Email || got.FirstName != user.FirstName || got.Gender != user.Gender {
		t.Fatalf("unexpected profile: %+v", got)
	}
}

func TestUpdateProfileRejectsFutureBirthDate(t *testing.T) {
	svc := NewService(&mocks.MockUserRepository{}, &mocks.MockRefreshTokenRepository{}, testSecret, time.Minute, time.Hour)
	_, err := svc.UpdateProfile(context.Background(), UpdateProfileInput{
		UserID: "u1", FirstName: "A", LastName: "B",
		Gender: entity.GenderFemale, BirthDate: time.Now().UTC().Add(24 * time.Hour).Format("2006-01-02"),
	})
	if err == nil {
		t.Fatal("expected future birth date to be rejected")
	}
}

func TestUpdateEmailRequiresMatchingOTPDestination(t *testing.T) {
	email := "new@example.com"
	users := &mocks.MockUserRepository{
		FindByEmailHashFunc: func(context.Context, string) (*entity.User, error) { return nil, nil },
	}
	otp := fakeOTPService{verify: func(_ context.Context, _ VerifyOTPInput) (string, error) {
		return "wrong-destination", nil
	}}
	svc := NewService(users, &mocks.MockRefreshTokenRepository{}, testSecret, time.Minute, time.Hour, otp)

	_, err := svc.UpdateEmail(context.Background(), UpdateEmailInput{
		UserID: "u1", Email: email, OTPChallengeID: "c1", OTPCode: "123456",
	})
	if err == nil {
		t.Fatal("expected destination mismatch to fail")
	}
}
