package entity

import "time"

// RecoverySession is a short-lived, one-time capability created after OTP +
// recovery-key verification. It lets a new device obtain the opaque recovery
// envelope without ever sending the recovery key or plaintext data key to the
// backend.
type RecoverySession struct {
	ID        string
	TokenHash string
	UserID    string
	CreatedAt time.Time
	ExpiresAt time.Time
	UsedAt    *time.Time
}
