package auth

import (
	"context"
	"github.com/mosinaRZ/finance-sync-backend/internal/domain/entity"
	"github.com/stretchr/testify/require"
	"testing"
	"time"
)

type otpRepoMock struct {
	c     *entity.OTPChallenge
	count int
}

func (r *otpRepoMock) Create(_ context.Context, o *entity.OTPChallenge) error { r.c = o; return nil }
func (r *otpRepoMock) FindActive(_ context.Context, _ string) (*entity.OTPChallenge, error) {
	return r.c, nil
}
func (r *otpRepoMock) Consume(_ context.Context, _ string) error {
	now := time.Now()
	r.c.ConsumedAt = &now
	return nil
}
func (r *otpRepoMock) IncrementAttempts(_ context.Context, _ string) error {
	r.c.Attempts++
	return nil
}
func (r *otpRepoMock) RecentCount(context.Context, string, string, entity.OTPPurpose, int) (int, error) {
	return r.count, nil
}

type deliveryMock struct{ code string }

func (d *deliveryMock) Send(_ context.Context, _ string, _ string, code string) error {
	d.code = code
	return nil
}
func TestOTPCodeIsNotStoredPlaintext(t *testing.T) {
	repo := &otpRepoMock{}
	d := &deliveryMock{}
	s := NewOTPService(repo, d, "test-secret", 5*time.Minute)
	out, err := s.Request(context.Background(), RequestOTPInput{Destination: "a@example.com", Channel: "email", Purpose: entity.OTPPurposePasswordReset})
	require.NoError(t, err)
	require.NotEmpty(t, out.ChallengeID)
	require.NotEmpty(t, repo.c.CodeHash)
	require.NotEqual(t, d.code, repo.c.CodeHash)

	got, err := s.Verify(context.Background(), VerifyOTPInput{
		ChallengeID: out.ChallengeID,
		Code:        d.code,
		Purpose:     entity.OTPPurposePasswordReset,
	})
	require.NoError(t, err)
	require.Equal(t, repo.c.DestinationHash, got)
}

func TestOTPCodeCannotBeReused(t *testing.T) {
	repo := &otpRepoMock{}
	d := &deliveryMock{}
	s := NewOTPService(repo, d, "test-secret", 5*time.Minute)
	out, err := s.Request(context.Background(), RequestOTPInput{
		Destination: "a@example.com",
		Channel:     "email",
		Purpose:     entity.OTPPurposePasswordReset,
	})
	require.NoError(t, err)

	_, err = s.Verify(context.Background(), VerifyOTPInput{
		ChallengeID: out.ChallengeID,
		Code:        d.code,
		Purpose:     entity.OTPPurposePasswordReset,
	})
	require.NoError(t, err)

	_, err = s.Verify(context.Background(), VerifyOTPInput{
		ChallengeID: out.ChallengeID,
		Code:        d.code,
		Purpose:     entity.OTPPurposePasswordReset,
	})
	require.Error(t, err)
}
