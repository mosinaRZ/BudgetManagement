package entity

import "time"

// RefreshToken represents a persisted refresh-token session.
// Persistence-specific tags belong to the infrastructure layer.
type RefreshToken struct {
	ID        string
	TokenHash string
	UserID    string
	DeviceID  string
	CreatedAt time.Time
	ExpiresAt time.Time
	RevokedAt *time.Time
}
