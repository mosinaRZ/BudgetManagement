package repository

import (
	"context"

	"github.com/mosinaRZ/finance-sync-backend/internal/domain/entity"
)

// UserRepository defines persistence operations for entity.User. Concrete
// implementations (e.g. backed by MongoDB) live in the infrastructure
// layer; this package stays free of any driver-specific dependency.
type UserRepository interface {
	// Create persists a new user. Implementations should return an error
	// (e.g. a conflict) if a user with the same PhoneHash already exists.
	Create(ctx context.Context, user *entity.User) error

	// FindByPhoneHash looks up a user by their phone hash, typically used
	// during authentication. Implementations should return a not-found
	// error if no matching user exists.
	FindByPhoneHash(ctx context.Context, phoneHash string) (*entity.User, error)

	// FindByID looks up a user by their ID. Implementations should return a
	// not-found error if no matching user exists.
	FindByID(ctx context.Context, id string) (*entity.User, error)

	// Update persists changes to an existing user.
	Update(ctx context.Context, user *entity.User) error

	// AddDevice associates a device ID with a user, e.g. when the user logs
	// in from a new device. Implementations should be idempotent with
	// respect to already-registered device IDs.
	AddDevice(ctx context.Context, userID, deviceID string) error
}
