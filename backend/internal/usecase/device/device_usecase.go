package device

import (
	"context"

	"github.com/mosinaRZ/finance-sync-backend/internal/domain/apperror"
	"github.com/mosinaRZ/finance-sync-backend/internal/domain/entity"
	"github.com/mosinaRZ/finance-sync-backend/internal/domain/repository"
)

type Service struct {
	devices repository.DeviceRepository
	refresh repository.RefreshTokenRepository
}

func NewService(devices repository.DeviceRepository, refresh repository.RefreshTokenRepository) *Service {
	return &Service{devices: devices, refresh: refresh}
}

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
// registered first. The primary device can remove every other device, but no other device
// can remove it.
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
// request, taken from the validated access token; it is empty for legacy tokens. The primary
// device may only be removed by itself, so an unknown caller can never remove it.
func (s *Service) Revoke(ctx context.Context, userID, callerDeviceID, targetDeviceID string) error {
	if userID == "" || targetDeviceID == "" || s == nil || s.devices == nil || s.refresh == nil {
		return apperror.ErrValidation("device information is required")
	}
	primary, err := s.devices.Primary(ctx, userID)
	if err != nil {
		return err
	}
	if primary != nil && primary.ID == targetDeviceID && callerDeviceID != targetDeviceID {
		return apperror.ErrPrimaryDeviceProtected("the primary device can only be removed from the primary device itself")
	}
	if err := s.devices.Revoke(ctx, userID, targetDeviceID); err != nil {
		return err
	}
	// Device revocation also invalidates every refresh token belonging to that device.
	return s.refresh.RevokeByUserAndDevice(ctx, userID, targetDeviceID)
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
