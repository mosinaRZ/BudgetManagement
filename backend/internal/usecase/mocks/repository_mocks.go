package mocks

import (
	"context"

	"github.com/mosinaRZ/finance-sync-backend/internal/domain/entity"
	"github.com/mosinaRZ/finance-sync-backend/internal/domain/repository"
)

// MockUserRepository پیاده‌سازی دستی UserRepository برای تست‌ها
type MockUserRepository struct {
	CreateFunc          func(ctx context.Context, u *entity.User) error
	FindByPhoneHashFunc func(ctx context.Context, phoneHash string) (*entity.User, error)
	FindByEmailHashFunc func(ctx context.Context, emailHash string) (*entity.User, error)
	FindByIDFunc        func(ctx context.Context, id string) (*entity.User, error)
	UpdateFunc          func(ctx context.Context, u *entity.User) error
	AddDeviceFunc       func(ctx context.Context, userID, deviceID string) error
}

func (m *MockUserRepository) Create(ctx context.Context, u *entity.User) error {
	if m.CreateFunc != nil {
		return m.CreateFunc(ctx, u)
	}
	return nil
}

func (m *MockUserRepository) FindByPhoneHash(ctx context.Context, phoneHash string) (*entity.User, error) {
	if m.FindByPhoneHashFunc != nil {
		return m.FindByPhoneHashFunc(ctx, phoneHash)
	}
	return nil, nil
}

func (m *MockUserRepository) FindByEmailHash(ctx context.Context, emailHash string) (*entity.User, error) {
	if m.FindByEmailHashFunc != nil {
		return m.FindByEmailHashFunc(ctx, emailHash)
	}
	return nil, nil
}

func (m *MockUserRepository) FindByID(ctx context.Context, id string) (*entity.User, error) {
	if m.FindByIDFunc != nil {
		return m.FindByIDFunc(ctx, id)
	}
	return nil, nil
}

func (m *MockUserRepository) Update(ctx context.Context, u *entity.User) error {
	if m.UpdateFunc != nil {
		return m.UpdateFunc(ctx, u)
	}
	return nil
}

func (m *MockUserRepository) AddDevice(ctx context.Context, userID, deviceID string) error {
	if m.AddDeviceFunc != nil {
		return m.AddDeviceFunc(ctx, userID, deviceID)
	}
	return nil
}

// MockSyncRepository پیاده‌سازی دستی SyncRepository برای تست‌ها
type MockSyncRepository struct {
	SyncFunc func(ctx context.Context, req repository.SyncRequest) (repository.SyncResult, error)
}

func (m *MockSyncRepository) Sync(ctx context.Context, req repository.SyncRequest) (repository.SyncResult, error) {
	if m.SyncFunc != nil {
		return m.SyncFunc(ctx, req)
	}
	return repository.SyncResult{}, nil
}

type MockRefreshTokenRepository struct {
	CreateFunc           func(context.Context, *entity.RefreshToken) error
	FindByHashFunc       func(context.Context, string) (*entity.RefreshToken, error)
	RevokeFunc           func(context.Context, string) error
	RevokeAllForUserFunc func(context.Context, string) error
}

func (m *MockRefreshTokenRepository) Create(ctx context.Context, t *entity.RefreshToken) error {
	if m.CreateFunc != nil {
		return m.CreateFunc(ctx, t)
	}
	return nil
}
func (m *MockRefreshTokenRepository) FindByHash(ctx context.Context, h string) (*entity.RefreshToken, error) {
	if m.FindByHashFunc != nil {
		return m.FindByHashFunc(ctx, h)
	}
	return nil, nil
}
func (m *MockRefreshTokenRepository) Revoke(ctx context.Context, h string) error {
	if m.RevokeFunc != nil {
		return m.RevokeFunc(ctx, h)
	}
	return nil
}
func (m *MockRefreshTokenRepository) RevokeAllForUser(ctx context.Context, id string) error {
	if m.RevokeAllForUserFunc != nil {
		return m.RevokeAllForUserFunc(ctx, id)
	}
	return nil
}
