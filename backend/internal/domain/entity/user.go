package entity

import "time"

type User struct {
	ID                  string
	Role                Role
	PhoneHash           string
	PhoneNumber         string
	PhoneVerified       bool
	FirstName           string
	LastName            string
	Gender              Gender
	BirthDate           string
	PasswordHash        string
	AuthSalt            string
	KdfSalt             string
	PasswordKeyEnvelope []byte
	PasswordKeyNonce    []byte
	RecoveryKeyHash     string
	RecoveryKeyEnvelope []byte
	RecoveryKeyNonce    []byte
	CreatedAt           time.Time
	UpdatedAt           time.Time
	Devices             []string
	FailedLoginAttempts int
	LockedUntil         *time.Time
	SessionVersion      uint64
	// IsDeleted marks a soft-deleted account. The document is kept (so the owner can restore it
	// during the grace period) but the account can no longer sign in, refresh or sync.
	IsDeleted bool
	// DeletedAt is when the account was soft-deleted; nil while the account is active.
	DeletedAt *time.Time
}

// RestorableUntil reports the last instant a soft-deleted account can still be restored.
func (u *User) RestorableUntil(window time.Duration) (time.Time, bool) {
	if u == nil || !u.IsDeleted || u.DeletedAt == nil {
		return time.Time{}, false
	}
	return u.DeletedAt.Add(window), true
}
