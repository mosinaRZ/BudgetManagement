package repository

import (
	"context"

	"github.com/mosinaRZ/finance-sync-backend/internal/domain/entity"
)

// SyncCursor is the server-issued monotonic revision cursor.
type SyncCursor uint64

type SyncRequest struct {
	RequestID string
	UserID    string
	DeviceID  string
	Cursor    SyncCursor
	Records   []*entity.SyncRecord
}

type SyncResult struct {
	ServerChanges []*entity.SyncRecord
	Conflicts     []*entity.SyncRecord
	NextCursor    SyncCursor
	HasMore       bool
}

// SyncRepository atomically applies one sync request and reads one bounded page.
// The request ID makes a committed request replay-safe.
type SyncRepository interface {
	Sync(ctx context.Context, req SyncRequest) (SyncResult, error)
}
