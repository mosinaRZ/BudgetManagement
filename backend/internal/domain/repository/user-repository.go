package repository

import (
	"context"
	"time"

	"github.com/mosinaRZ/finance-sync-backend/internal/domain/entity"
)

type UserRepository interface {
	Create(context.Context, *entity.User) error
	FindByPhoneHash(context.Context, string) (*entity.User, error)
	FindByEmailHash(context.Context, string) (*entity.User, error)
	FindByID(context.Context, string) (*entity.User, error)
	Update(context.Context, *entity.User) error
	UpdateProfile(context.Context, string, string, string, entity.Gender, string) error
	UpdateEmail(context.Context, string, string, string, bool) error
	UpdateCredentials(context.Context, string, string, string, string, []byte, []byte) error
	AddDevice(context.Context, string, string) error
	UpdateRole(context.Context, string, entity.Role) error
	RecordFailedLogin(context.Context, string, time.Time, int, time.Duration) error
	ResetFailedLogin(context.Context, string) error
	GetSessionVersion(context.Context, string) (uint64, error)
	IncrementSessionVersion(context.Context, string) (uint64, error)
}
