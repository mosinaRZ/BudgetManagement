package auth

import (
	"context"
	"crypto/hmac"
	"crypto/rand"
	"crypto/sha256"
	"encoding/base64"
	"fmt"
	"github.com/mosinaRZ/finance-sync-backend/internal/domain/apperror"
	"github.com/mosinaRZ/finance-sync-backend/internal/domain/entity"
	"github.com/mosinaRZ/finance-sync-backend/internal/domain/repository"
	"go.mongodb.org/mongo-driver/bson/primitive"
	"net/mail"
	"strings"
	"time"
)

type OTPDelivery interface {
	Send(context.Context, string, string, string) error
}
type otpService struct {
	repo        repository.OTPRepository
	delivery    OTPDelivery
	secret      string
	ttl         time.Duration
	maxAttempts int
}

func NewOTPService(repo repository.OTPRepository, d OTPDelivery, secret string, ttl time.Duration) *otpService {
	return &otpService{repo: repo, delivery: d, secret: secret, ttl: ttl, maxAttempts: 5}
}
func hashDestination(v, secret string) string { return hashIdentifier(v, secret) }
func hashOTP(id, code, secret string) string {
	h := sha256.Sum256([]byte(secret + ":" + id + ":" + code))
	return base64.RawURLEncoding.EncodeToString(h[:])
}
func (s *otpService) Request(ctx context.Context, in RequestOTPInput) (RequestOTPOutput, error) {
	if strings.TrimSpace(s.secret) == "" || s.ttl <= 0 {
		return RequestOTPOutput{}, apperror.ErrInternal("OTP service is not configured")
	}
	d := strings.TrimSpace(in.Destination)
	ch := strings.ToLower(strings.TrimSpace(in.Channel))
	if d == "" || (ch != "sms" && ch != "email") {
		return RequestOTPOutput{}, apperror.ErrValidation("invalid OTP destination")
	}
	if ch == "sms" {
		normalized, err := normalizePhone(d)
		if err != nil {
			return RequestOTPOutput{}, apperror.ErrValidation("invalid phone number")
		}
		d = normalized
	} else {
		addr, err := mail.ParseAddress(d)
		if err != nil || addr.Address != d {
			return RequestOTPOutput{}, apperror.ErrValidation("invalid email address")
		}
	}
	if in.Purpose != entity.OTPPurposeRegister && in.Purpose != entity.OTPPurposePasswordReset && in.Purpose != entity.OTPPurposeEmailVerification && in.Purpose != entity.OTPPurposeLogin {
		return RequestOTPOutput{}, apperror.ErrValidation("OTP purpose is required")
	}
	dh := hashDestination(d, s.secret)
	n, err := s.repo.RecentCount(ctx, dh, ch, in.Purpose, 10)
	if err != nil {
		return RequestOTPOutput{}, err
	}
	if n >= 3 {
		return RequestOTPOutput{}, apperror.ErrRateLimited("too many OTP requests")
	}
	b := make([]byte, 4)
	if _, err := rand.Read(b); err != nil {
		return RequestOTPOutput{}, apperror.ErrInternal("failed to generate OTP")
	}
	code := fmt.Sprintf("%06d", (uint32(b[0])<<24|uint32(b[1])<<16|uint32(b[2])<<8|uint32(b[3]))%1000000)
	id := primitive.NewObjectID().Hex()
	now := time.Now().UTC()
	o := &entity.OTPChallenge{ID: id, DestinationHash: dh, Channel: ch, Purpose: in.Purpose, CodeHash: hashOTP(id, code, s.secret), CreatedAt: now, ExpiresAt: now.Add(s.ttl)}
	if err := s.repo.Create(ctx, o); err != nil {
		return RequestOTPOutput{}, err
	}
	if err := s.delivery.Send(ctx, d, ch, code); err != nil {
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
	if o.ConsumedAt != nil || time.Now().UTC().After(o.ExpiresAt) || o.Attempts >= s.maxAttempts {
		return "", apperror.ErrUnauthorized("invalid OTP")
	}
	if err := s.repo.IncrementAttempts(ctx, o.ID); err != nil {
		return "", err
	}
	if !hmac.Equal([]byte(hashOTP(o.ID, strings.TrimSpace(in.Code), s.secret)), []byte(o.CodeHash)) {
		return "", apperror.ErrUnauthorized("invalid OTP")
	}
	if err := s.repo.Consume(ctx, o.ID); err != nil {
		return "", apperror.ErrUnauthorized("invalid OTP")
	}
	return o.DestinationHash, nil
}
