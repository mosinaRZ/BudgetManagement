package auth

import (
	"context"
	"crypto/hmac"
	"crypto/rand"
	"crypto/sha256"
	"encoding/base64"
	"fmt"
	"strings"
	"time"

	"github.com/mosinaRZ/finance-sync-backend/internal/domain/apperror"
	"github.com/mosinaRZ/finance-sync-backend/internal/domain/entity"
	"github.com/mosinaRZ/finance-sync-backend/internal/domain/repository"
	"go.mongodb.org/mongo-driver/bson/primitive"
)

type OTPDelivery interface {
	Send(context.Context, string, string, string) error
}

type otpService struct {
	repo        repository.OTPRepository
	delivery    OTPDelivery
	keys        Keys
	ttl         time.Duration
	maxAttempts int
}

// NewOTPService needs Keys.PhoneHashKey (the destination hash must equal the account's phone
// hash) and Keys.OTPCodeKey (code hashes). It never uses the JWT secret.
func NewOTPService(repo repository.OTPRepository, d OTPDelivery, keys Keys, ttl time.Duration) *otpService {
	return &otpService{repo: repo, delivery: d, keys: keys, ttl: ttl, maxAttempts: 5}
}

func hashDestination(v, phoneHashKey string) string { return hashIdentifier(v, phoneHashKey) }

// hashOTP is HMAC-SHA256 under the dedicated OTP key, bound to the challenge id.
func hashOTP(id, code, otpKey string) string {
	mac := hmac.New(sha256.New, []byte(otpKey))
	_, _ = mac.Write([]byte("cidna/v1/otp|" + id + "|" + code))
	return base64.RawURLEncoding.EncodeToString(mac.Sum(nil))
}

func (s *otpService) Request(ctx context.Context, in RequestOTPInput) (RequestOTPOutput, error) {
	if strings.TrimSpace(s.keys.PhoneHashKey) == "" || strings.TrimSpace(s.keys.OTPCodeKey) == "" || s.ttl <= 0 {
		return RequestOTPOutput{}, apperror.ErrInternal("OTP service is not configured")
	}

	d := strings.TrimSpace(in.Destination)
	ch := strings.ToLower(strings.TrimSpace(in.Channel))
	if d == "" || ch != "sms" {
		return RequestOTPOutput{}, apperror.ErrValidation("invalid OTP destination")
	}
	if ch == "sms" {
		normalized, err := normalizePhone(d)
		if err != nil {
			return RequestOTPOutput{}, apperror.ErrValidation("invalid phone number")
		}
		d = normalized
	}

	if in.Purpose != entity.OTPPurposeRegister && in.Purpose != entity.OTPPurposePasswordReset && in.Purpose != entity.OTPPurposeLogin {
		return RequestOTPOutput{}, apperror.ErrValidation("OTP purpose is required")
	}

	dh := hashDestination(d, s.keys.PhoneHashKey)
	n, err := s.repo.RecentCount(ctx, dh, ch, in.Purpose, 10)
	if err != nil {
		return RequestOTPOutput{}, err
	}
	if n >= 3 {
		return RequestOTPOutput{}, apperror.ErrRateLimited("too many OTP requests")
	}

	// Only the latest active challenge for a destination/purpose can be used.
	// This prevents an old code from remaining valid after a resend.
	if err := s.repo.InvalidateActive(ctx, dh, ch, in.Purpose); err != nil {
		return RequestOTPOutput{}, err
	}

	b := make([]byte, 4)
	if _, err := rand.Read(b); err != nil {
		return RequestOTPOutput{}, apperror.ErrInternal("failed to generate OTP")
	}
	code := fmt.Sprintf("%06d", (uint32(b[0])<<24|uint32(b[1])<<16|uint32(b[2])<<8|uint32(b[3]))%1000000)
	id := primitive.NewObjectID().Hex()
	now := time.Now().UTC()
	o := &entity.OTPChallenge{
		ID: id, DestinationHash: dh, Channel: ch, Purpose: in.Purpose,
		CodeHash: hashOTP(id, code, s.keys.OTPCodeKey), CreatedAt: now, ExpiresAt: now.Add(s.ttl),
	}
	if err := s.repo.Create(ctx, o); err != nil {
		return RequestOTPOutput{}, err
	}
	if err := s.delivery.Send(ctx, d, ch, code); err != nil {
		// Do not leave a challenge usable when delivery failed.
		_ = s.repo.InvalidateByID(ctx, id)
		return RequestOTPOutput{}, apperror.ErrInternal("failed to deliver OTP", err)
	}
	return RequestOTPOutput{ChallengeID: id, ExpiresAt: o.ExpiresAt.Unix()}, nil
}

func (s *otpService) Verify(ctx context.Context, in VerifyOTPInput) (string, error) {
	o, err := s.repo.FindActive(ctx, in.ChallengeID)
	if err != nil {
		return "", apperror.ErrUnauthorized("invalid OTP")
	}
	if in.Purpose != "" && o.Purpose != in.Purpose {
		return "", apperror.ErrUnauthorized("invalid OTP")
	}

	now := time.Now().UTC()
	if o.ConsumedAt != nil || now.After(o.ExpiresAt) || o.Attempts >= s.maxAttempts {
		return "", apperror.ErrUnauthorized("invalid OTP")
	}

	ok, err := s.repo.VerifyAndConsume(ctx, o.ID, hashOTP(o.ID, strings.TrimSpace(in.Code), s.keys.OTPCodeKey), now, s.maxAttempts)
	if err != nil {
		return "", err
	}
	if !ok {
		return "", apperror.ErrUnauthorized("invalid OTP")
	}

	return o.DestinationHash, nil
}
