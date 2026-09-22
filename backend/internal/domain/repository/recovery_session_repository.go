package repository

import (
	"context"
	"time"

	"github.com/mosinaRZ/finance-sync-backend/internal/domain/entity"
)

type RecoverySessionRepository interface {
	Create(context.Context, *entity.RecoverySession) error
	Consume(context.Context, string, time.Time) (*entity.RecoverySession, error)
}
