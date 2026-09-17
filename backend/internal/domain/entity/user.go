package entity

import "time"

type User struct {
	ID                  string
	Role                Role
	PhoneHash           string
	EmailHash           string
	PhoneVerified       bool
	EmailVerified       bool
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
}
