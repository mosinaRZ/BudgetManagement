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
		ID: "u1", PhoneNumber: "+989121234567",
		FirstName: "Ali", LastName: "Ahmadi",
		Gender: entity.GenderMale, BirthDate: "2000-01-02",
	}
	svc := NewService(&mocks.MockUserRepository{
		FindByIDFunc: func(context.Context, string) (*entity.User, error) { return user, nil },
	}, &mocks.MockRefreshTokenRepository{}, testKeys, time.Minute, time.Hour)

	got, err := svc.GetProfile(context.Background(), "u1")
	if err != nil {
		t.Fatal(err)
	}
	if got.FirstName != user.FirstName || got.Gender != user.Gender {
		t.Fatalf("unexpected profile: %+v", got)
	}
}

func TestUpdateProfileRejectsFutureBirthDate(t *testing.T) {
	svc := NewService(&mocks.MockUserRepository{}, &mocks.MockRefreshTokenRepository{}, testKeys, time.Minute, time.Hour)
	_, err := svc.UpdateProfile(context.Background(), UpdateProfileInput{
		UserID: "u1", FirstName: "A", LastName: "B",
		Gender: entity.GenderFemale, BirthDate: time.Now().UTC().Add(24 * time.Hour).Format("2006-01-02"),
	})
	if err == nil {
		t.Fatal("expected future birth date to be rejected")
	}
}
