package repository

import (
	"context"
	"time"

	"github.com/mosinaRZ/finance-sync-backend/internal/domain/entity"
)

type OTPRepository interface {
	Create(context.Context, *entity.OTPChallenge) error
	FindActive(context.Context, string) (*entity.OTPChallenge, error)
	InvalidateActive(context.Context, string, string, entity.OTPPurpose) error
	InvalidateByID(context.Context, string) error
	VerifyAndConsume(context.Context, string, string, time.Time, int) (bool, error)
	RecentCount(context.Context, string, string, entity.OTPPurpose, int) (int, error)
}
