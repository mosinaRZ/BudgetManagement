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

func (s *Service) List(ctx context.Context, userID string) ([]*entity.Device, error) {
	if userID == "" || s == nil || s.devices == nil {
		return nil, apperror.ErrUnauthorized("invalid user context")
	}
	return s.devices.List(ctx, userID)
}

func (s *Service) Revoke(ctx context.Context, userID, deviceID string) error {
	if userID == "" || deviceID == "" || s == nil || s.devices == nil || s.refresh == nil {
		return apperror.ErrValidation("device information is required")
	}
	if err := s.devices.Revoke(ctx, userID, deviceID); err != nil {
		return err
	}
	// Device revocation also invalidates every refresh token belonging to that device.
	return s.refresh.RevokeByUserAndDevice(ctx, userID, deviceID)
}
