package repository

import (
	"context"
	"time"

	"github.com/mosinaRZ/finance-sync-backend/internal/domain/entity"
)

// DeviceEventRepository stores the per-account device activity feed.
type DeviceEventRepository interface {
	// Record appends an event to the user's feed.
	Record(ctx context.Context, event *entity.DeviceEvent) error

	// ListAfter returns the user's events created strictly after the given instant, oldest
	// first, capped at limit.
	ListAfter(ctx context.Context, userID string, after time.Time, limit int) ([]*entity.DeviceEvent, error)
}
