package device

import (
	"context"
	"log/slog"
	"time"

	"github.com/mosinaRZ/finance-sync-backend/internal/domain/apperror"
	"github.com/mosinaRZ/finance-sync-backend/internal/domain/entity"
	"github.com/mosinaRZ/finance-sync-backend/internal/domain/repository"
)

// maxEventsPerPoll bounds one response of the device activity feed. A client that is further
// behind simply asks again from the last event it received.
const maxEventsPerPoll = 50

type Service struct {
	devices repository.DeviceRepository
	refresh repository.RefreshTokenRepository
	events  repository.DeviceEventRepository
}

func NewService(devices repository.DeviceRepository, refresh repository.RefreshTokenRepository) *Service {
	return &Service{devices: devices, refresh: refresh}
}

// SetEventRepository enables the device activity feed. Without it removals still work; they
// just leave no trace for the other devices to be notified about.
func (s *Service) SetEventRepository(events repository.DeviceEventRepository) { s.events = events }

// Overview is the device list together with the id of the account's primary device.
type Overview struct {
	Devices   []*entity.Device
	PrimaryID string
}

func (s *Service) List(ctx context.Context, userID string) ([]*entity.Device, error) {
	if userID == "" || s == nil || s.devices == nil {
		return nil, apperror.ErrUnauthorized("invalid user context")
	}
	return s.devices.List(ctx, userID)
}

// Overview lists the user's devices and identifies the primary one: the device that was
// registered first. Only the primary device can remove other devices.
func (s *Service) Overview(ctx context.Context, userID string) (Overview, error) {
	list, err := s.List(ctx, userID)
	if err != nil {
		return Overview{}, err
	}
	primary, err := s.devices.Primary(ctx, userID)
	if err != nil {
		return Overview{}, err
	}
	if primary != nil {
		return Overview{Devices: list, PrimaryID: primary.ID}, nil
	}
	return Overview{Devices: list, PrimaryID: earliest(list)}, nil
}

// Revoke removes targetDeviceID from the account. callerDeviceID is the device making the
// request, taken from the validated access token; it is empty for legacy tokens.
//
// The rules are deliberately strict and fail closed:
//   - any device may remove itself (that is a sign-out);
//   - only the primary (first-registered) device may remove a different device;
//   - every other combination is refused, including calls from a caller that cannot be
//     identified. The primary device is reported with its own error code so clients can
//     explain that it can only be removed from itself.
func (s *Service) Revoke(ctx context.Context, userID, callerDeviceID, targetDeviceID string) error {
	if userID == "" || targetDeviceID == "" || s == nil || s.devices == nil || s.refresh == nil {
		return apperror.ErrValidation("device information is required")
	}
	primary, err := s.devices.Primary(ctx, userID)
	if err != nil {
		return err
	}
	removingSelf := callerDeviceID != "" && callerDeviceID == targetDeviceID
	if !removingSelf {
		callerIsPrimary := primary != nil && callerDeviceID != "" && primary.ID == callerDeviceID
		if !callerIsPrimary {
			if primary != nil && primary.ID == targetDeviceID {
				return apperror.ErrPrimaryDeviceProtected("the primary device can only be removed from the primary device itself")
			}
			return apperror.ErrDeviceRemovalForbidden("only the primary device can remove other devices")
		}
	}

	// Snapshot the descriptive details before the row disappears so the activity feed can
	// still say which device was removed and by whom.
	var target, caller *entity.Device
	if s.events != nil && !removingSelf {
		if list, listErr := s.devices.List(ctx, userID); listErr == nil {
			for _, d := range list {
				switch {
				case d == nil:
				case d.ID == targetDeviceID:
					target = d
				case d.ID == callerDeviceID:
					caller = d
				}
			}
		}
	}

	if err := s.devices.Revoke(ctx, userID, targetDeviceID); err != nil {
		return err
	}
	// Device revocation also invalidates every refresh token belonging to that device.
	if err := s.refresh.RevokeByUserAndDevice(ctx, userID, targetDeviceID); err != nil {
		return err
	}
	if !removingSelf {
		// Signing yourself out is not something the other devices need to be alerted about.
		s.recordRemoval(ctx, userID, targetDeviceID, target, callerDeviceID, caller)
	}
	return nil
}

// recordRemoval is best-effort: the device is already gone, so a failure to log the event
// must not turn a successful removal into an error.
func (s *Service) recordRemoval(ctx context.Context, userID, targetID string, target *entity.Device, callerID string, caller *entity.Device) {
	if s.events == nil {
		return
	}
	event := &entity.DeviceEvent{
		UserID:        userID,
		Type:          entity.DeviceEventRemoved,
		DeviceID:      targetID,
		DeviceName:    target.DisplayName(),
		ActorDeviceID: callerID,
		ActorName:     caller.DisplayName(),
		CreatedAt:     time.Now().UTC(),
	}
	if target != nil {
		event.Platform = target.Platform
	}
	if err := s.events.Record(ctx, event); err != nil {
		slog.Warn("failed to record device removal event", "user_id", userID, "device_id", targetID, "error", err)
	}
}

// EventFeed is one page of the device activity feed.
type EventFeed struct {
	// Events are the entries the calling device should be told about, oldest first.
	Events []*entity.DeviceEvent
	// Cursor is the CreatedAt of the newest entry that was examined, including entries that were
	// filtered out because they concern the caller itself. Clients continue from it, so a page
	// consisting only of filtered entries can never stall the feed. It is the zero time when
	// nothing was examined.
	Cursor time.Time
}

// Events returns the device activity the calling device has not been told about yet: devices
// that signed in and devices that were removed after `after`. A device only hears about
// things that happened after it joined the account, and never about its own actions.
func (s *Service) Events(ctx context.Context, userID, callerDeviceID string, after time.Time) (EventFeed, error) {
	if userID == "" || s == nil || s.devices == nil {
		return EventFeed{}, apperror.ErrUnauthorized("invalid user context")
	}
	if s.events == nil || callerDeviceID == "" {
		return EventFeed{}, nil
	}
	list, err := s.devices.List(ctx, userID)
	if err != nil {
		return EventFeed{}, err
	}
	var joinedAt time.Time
	found := false
	for _, d := range list {
		if d != nil && d.ID == callerDeviceID {
			joinedAt, found = d.CreatedAt, true
			break
		}
	}
	if !found {
		return EventFeed{}, nil
	}
	if joinedAt.After(after) {
		after = joinedAt
	}
	events, err := s.events.ListAfter(ctx, userID, after, maxEventsPerPoll)
	if err != nil {
		return EventFeed{}, err
	}
	feed := EventFeed{Events: make([]*entity.DeviceEvent, 0, len(events))}
	for _, e := range events {
		if e == nil {
			continue
		}
		if e.CreatedAt.After(feed.Cursor) {
			feed.Cursor = e.CreatedAt
		}
		if e.DeviceID == callerDeviceID || e.ActorDeviceID == callerDeviceID {
			continue
		}
		feed.Events = append(feed.Events, e)
	}
	return feed, nil
}

func earliest(list []*entity.Device) string {
	var best *entity.Device
	for _, d := range list {
		if d == nil {
			continue
		}
		if best == nil || d.CreatedAt.Before(best.CreatedAt) || (d.CreatedAt.Equal(best.CreatedAt) && d.ID < best.ID) {
			best = d
		}
	}
	if best == nil {
		return ""
	}
	return best.ID
}
