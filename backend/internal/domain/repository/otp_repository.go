package repository

import (
	"context"
	"github.com/mosinaRZ/finance-sync-backend/internal/domain/entity"
)

type OTPRepository interface {
	Create(context.Context, *entity.OTPChallenge) error
	FindActive(context.Context, string) (*entity.OTPChallenge, error)
	Consume(context.Context, string) error
	IncrementAttempts(context.Context, string) error
	RecentCount(context.Context, string, string, entity.OTPPurpose, int) (int, error)
}
