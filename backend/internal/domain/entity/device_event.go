package entity

import "time"

// DeviceEventType classifies an entry of an account's device activity feed.
type DeviceEventType string

const (
	// DeviceEventSignedIn is recorded when a device that was not yet part of the account signs in.
	DeviceEventSignedIn DeviceEventType = "signed_in"
	// DeviceEventRemoved is recorded when a device is removed from the account.
	DeviceEventRemoved DeviceEventType = "removed"
)

// DeviceEvent is one entry of the account's device activity feed. The feed is how the
// account's other devices learn that something security-relevant happened (a new device
// signed in, a device was removed) so they can alert the user, without needing a push
// channel: each client simply asks for the events it has not seen yet.
type DeviceEvent struct {
	// ID is the unique identifier of the event.
	ID string

	// UserID is the owning user's ID.
	UserID string

	// Type says what happened.
	Type DeviceEventType

	// DeviceID is the device the event is about: the one that signed in, or the one that was removed.
	DeviceID string

	// DeviceName and Platform describe that device. They are display-only snapshots taken when
	// the event happened, so the feed stays readable after the device row is gone.
	DeviceName string
	Platform   string

	// ActorDeviceID and ActorName identify the device that caused the event. For a sign-in this
	// is the signing-in device itself; for a removal it is the device that performed the removal.
	ActorDeviceID string
	ActorName     string

	// CreatedAt is when the event happened.
	CreatedAt time.Time
}
