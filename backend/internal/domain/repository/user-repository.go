package repository

import (
	"context"
	"time"

	"github.com/mosinaRZ/finance-sync-backend/internal/domain/entity"
)

type UserRepository interface {
	Create(context.Context, *entity.User) error
	FindByPhoneHash(context.Context, string) (*entity.User, error)
	FindByID(context.Context, string) (*entity.User, error)
	Update(context.Context, *entity.User) error
	UpdateProfile(context.Context, string, string, string, entity.Gender, string) error
	UpdateCredentials(context.Context, string, string, string, string, []byte, []byte) error
	AddDevice(context.Context, string, string) error
	UpdateRole(context.Context, string, entity.Role) error
	RecordFailedLogin(context.Context, string, time.Time, int, time.Duration) error
	ResetFailedLogin(context.Context, string) error
	GetSessionVersion(context.Context, string) (uint64, error)
	IncrementSessionVersion(context.Context, string) (uint64, error)
	// SetDeleted soft-deletes (deleted=true) or restores (deleted=false) an account. In both
	// directions it bumps the session version so every previously issued access token stops
	// working, and it returns the new version.
	SetDeleted(ctx context.Context, userID string, deleted bool, at time.Time) (uint64, error)
}
