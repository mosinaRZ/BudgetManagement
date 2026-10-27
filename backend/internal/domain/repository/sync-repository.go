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

// SyncQuota bounds what one account may keep on the server. A request that would push the
// account over a limit is rejected as a whole (apperror.CodeStorageQuotaExceeded); a request that
// does not grow the account (updates that shrink data, deletions) is always accepted, so a full
// account can always free space. Zero disables the corresponding limit.
type SyncQuota struct {
	// MaxRecords counts every stored record of the account, tombstones included.
	MaxRecords int64
	// MaxBytes counts ciphertext plus nonce bytes of all stored records.
	MaxBytes int64
}

// Default per-account limits. A personal budget app needs a tiny fraction of this: an encrypted
// transaction is well below 2 KiB.
const (
	DefaultSyncMaxRecordsPerAccount int64 = 50_000
	DefaultSyncMaxBytesPerAccount   int64 = 50 << 20 // 50 MiB
)

// DefaultSyncQuota returns the built-in per-account limits.
func DefaultSyncQuota() SyncQuota {
	return SyncQuota{MaxRecords: DefaultSyncMaxRecordsPerAccount, MaxBytes: DefaultSyncMaxBytesPerAccount}
}

// SyncRepository atomically applies one sync request and reads one bounded page.
// The request ID makes a committed request replay-safe.
type SyncRepository interface {
	Sync(ctx context.Context, req SyncRequest) (SyncResult, error)
}
