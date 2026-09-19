package entity

import "time"

// Device represents a single client device (Android install) that a user
// has authenticated from. It allows the system to track multi-device usage
// and to attribute sync records to the device that produced them.
type Device struct {
	// ID is the unique identifier of the device (client-generated or
	// server-issued at registration time).
	ID string

	// UserID is the owning user's ID.
	UserID string

	// LastSeenAt is the timestamp of the device's most recent activity
	// (e.g. last successful sync or authentication).
	LastSeenAt time.Time

	// CreatedAt is the timestamp the device was first registered.
	CreatedAt time.Time
}
