package repository

import (
	"context"

	"github.com/mosinaRZ/finance-sync-backend/internal/domain/entity"
)

type RefreshTokenRepository interface {
	Create(ctx context.Context, token *entity.RefreshToken) error
	FindByHash(ctx context.Context, hash string) (*entity.RefreshToken, error)
	Revoke(ctx context.Context, hash string) error
	RevokeAllForUser(ctx context.Context, userID string) error
	RevokeByUserAndDevice(ctx context.Context, userID, deviceID string) error
}
