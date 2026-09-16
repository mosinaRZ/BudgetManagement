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
}
