package repository

import (
	"context"
	"time"

	"github.com/mosinaRZ/finance-sync-backend/internal/domain/entity"
)

// SecurityEventFilter narrows a query of the security event log.
type SecurityEventFilter struct {
	Types    []string
	Severity string
	UserID   string
	From     time.Time
	To       time.Time
	Limit    int
	Offset   int
}

// SecurityEventRepository stores the append-only security trail shown in the admin console.
type SecurityEventRepository interface {
	Record(ctx context.Context, e *entity.SecurityEvent) error
	// List returns newest-first events and the total number matching the filter.
	List(ctx context.Context, f SecurityEventFilter) ([]*entity.SecurityEvent, int64, error)
	// Between returns up to max events (newest first) inside [from, to) for aggregation.
	Between(ctx context.Context, from, to time.Time, f SecurityEventFilter, max int) ([]*entity.SecurityEvent, error)
}

// AdminUserFilter is the query for the user-management list.
type AdminUserFilter struct {
	Search       string // phone fragment, name fragment or exact user id
	Role         string // "", "user", "admin"
	Status       string // "", "active", "deleted", "locked"
	Presence     string // "", "online", "offline"
	Sort         string // "newest" (default), "oldest", "name"
	Limit        int
	Offset       int
	OnlineWindow time.Duration
}

// AdminUserRow is a user together with the facts derived from other collections.
type AdminUserRow struct {
	User        *entity.User
	Devices     []*entity.Device
	LastSeenAt  time.Time
	Online      bool
	SyncRecords int64
	SyncBytes   int64
}

// Bucket is one labelled count in a distribution.
type Bucket struct {
	Key   string
	Count int64
}

// DayPoint is one day of a time series (Day is YYYY-MM-DD in the requested offset).
type DayPoint struct {
	Day     string
	Records int64
	Bytes   int64
}

// SyncUserUsage is one row of the "heaviest syncers" list.
type SyncUserUsage struct {
	UserID  string
	Records int64
	Bytes   int64
}

// AdminOverview holds the account-level totals and distributions.
type AdminOverview struct {
	TotalUsers   int64
	ActiveUsers  int64
	DeletedUsers int64
	LockedUsers  int64
	OnlineNow    int64
	NewInPeriod  int64
	SignupsByDay []DayPoint // Records = signups
	Roles        []Bucket
	Platforms    []Bucket
	AppVersions  []Bucket
	SyncRecords  int64
	SyncBytes    int64
	SyncByDay    []DayPoint
	SyncByEntity []Bucket
	TopSyncUsers []SyncUserUsage
}

// AdminQueryRepository is the read side of the admin console. It never returns credential
// material: callers get entity.User values and must not expose the hashes/envelopes.
type AdminQueryRepository interface {
	ListUsers(ctx context.Context, f AdminUserFilter) ([]AdminUserRow, int64, error)
	GetUser(ctx context.Context, id string, onlineWindow time.Duration) (*AdminUserRow, error)
	CountActiveAdmins(ctx context.Context) (int64, error)
	Overview(ctx context.Context, from, to time.Time, tzOffsetMin int, onlineWindow time.Duration, scanCap int64) (AdminOverview, error)
	// Labels maps user ids to a display label (name or phone) for the event log.
	Labels(ctx context.Context, ids []string) (map[string]string, error)
}
