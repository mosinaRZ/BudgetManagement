package repository

import (
	"context"

	"github.com/mosinaRZ/finance-sync-backend/internal/domain/entity"
)

type DeviceRepository interface {
	Register(context.Context, *entity.Device) error
	List(context.Context, string) ([]*entity.Device, error)
	Revoke(context.Context, string, string) error
	Exists(context.Context, string, string) (bool, error)
	Touch(context.Context, string, string) error
	// Primary returns the user's earliest-registered device (the account's primary device),
	// or (nil, nil) when the user has no devices.
	Primary(context.Context, string) (*entity.Device, error)
}
