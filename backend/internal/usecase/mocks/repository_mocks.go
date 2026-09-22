package mocks

import (
	"context"
	"time"

	"github.com/mosinaRZ/finance-sync-backend/internal/domain/entity"
	"github.com/mosinaRZ/finance-sync-backend/internal/domain/repository"
)

// MockUserRepository پیاده‌سازی دستی UserRepository برای تست‌ها
type MockUserRepository struct {
	CreateFunc                  func(ctx context.Context, u *entity.User) error
	FindByPhoneHashFunc         func(ctx context.Context, phoneHash string) (*entity.User, error)
	FindByEmailHashFunc         func(ctx context.Context, emailHash string) (*entity.User, error)
	FindByIDFunc                func(ctx context.Context, id string) (*entity.User, error)
	UpdateFunc                  func(ctx context.Context, u *entity.User) error
	AddDeviceFunc               func(ctx context.Context, userID, deviceID string) error
	UpdateRoleFunc              func(ctx context.Context, userID string, role entity.Role) error
	RecordFailedLoginFunc       func(ctx context.Context, userID string, now time.Time, threshold int, lockDuration time.Duration) error
	ResetFailedLoginFunc        func(ctx context.Context, userID string) error
	GetSessionVersionFunc       func(ctx context.Context, userID string) (uint64, error)
	IncrementSessionVersionFunc func(ctx context.Context, userID string) (uint64, error)
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

func (m *MockUserRepository) UpdateCredentials(ctx context.Context, userID, passwordHash, authSalt, kdfSalt string, passwordKeyEnvelope, passwordKeyNonce []byte) error {
	if m.UpdateCredentialsFunc != nil {
		return m.UpdateCredentialsFunc(ctx, userID, passwordHash, authSalt, kdfSalt, passwordKeyEnvelope, passwordKeyNonce)
	}
	return nil
}

func (m *MockUserRepository) UpdateRole(ctx context.Context, userID string, role entity.Role) error {
	if m.UpdateRoleFunc != nil {
		return m.UpdateRoleFunc(ctx, userID, role)
	}
	return nil
}

func (m *MockUserRepository) RecordFailedLogin(ctx context.Context, userID string, now time.Time, threshold int, lockDuration time.Duration) error {
	if m.RecordFailedLoginFunc != nil {
		return m.RecordFailedLoginFunc(ctx, userID, now, threshold, lockDuration)
	}
	return nil
}
func (m *MockUserRepository) ResetFailedLogin(ctx context.Context, userID string) error {
	if m.ResetFailedLoginFunc != nil {
		return m.ResetFailedLoginFunc(ctx, userID)
	}
	return nil
}

func (m *MockUserRepository) GetSessionVersion(ctx context.Context, userID string) (uint64, error) {
	if m.GetSessionVersionFunc != nil {
		return m.GetSessionVersionFunc(ctx, userID)
	}
	return 0, nil
}
func (m *MockUserRepository) IncrementSessionVersion(ctx context.Context, userID string) (uint64, error) {
	if m.IncrementSessionVersionFunc != nil {
		return m.IncrementSessionVersionFunc(ctx, userID)
	}
	return 0, nil
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
	CreateFunc                func(context.Context, *entity.RefreshToken) error
	FindByHashFunc            func(context.Context, string) (*entity.RefreshToken, error)
	RevokeFunc                func(context.Context, string) error
	RevokeAllForUserFunc      func(context.Context, string) error
	RevokeByUserAndDeviceFunc func(context.Context, string, string) error
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
func (m *MockRefreshTokenRepository) RevokeByUserAndDevice(ctx context.Context, userID, deviceID string) error {
	if m.RevokeByUserAndDeviceFunc != nil {
		return m.RevokeByUserAndDeviceFunc(ctx, userID, deviceID)
	}
	return nil
}
func (m *MockRefreshTokenRepository) RevokeAllForUser(ctx context.Context, id string) error {
	if m.RevokeAllForUserFunc != nil {
		return m.RevokeAllForUserFunc(ctx, id)
	}
	return nil
}

type MockDeviceRepository struct {
	ListFunc   func(context.Context, string) ([]*entity.Device, error)
	RevokeFunc func(context.Context, string, string) error
}

func (m *MockDeviceRepository) Register(context.Context, *entity.Device) error { return nil }
func (m *MockDeviceRepository) List(ctx context.Context, id string) ([]*entity.Device, error) {
	if m.ListFunc != nil {
		return m.ListFunc(ctx, id)
	}
	return nil, nil
}
func (m *MockDeviceRepository) Revoke(ctx context.Context, u, d string) error {
	if m.RevokeFunc != nil {
		return m.RevokeFunc(ctx, u, d)
	}
	return nil
}
func (m *MockDeviceRepository) Exists(context.Context, string, string) (bool, error) {
	return true, nil
}
func (m *MockDeviceRepository) Touch(context.Context, string, string) error { return nil }

type MockAuditLogRepository struct {
	CreateFunc func(context.Context, *entity.AuditLog) error
}

func (m *MockAuditLogRepository) Create(ctx context.Context, l *entity.AuditLog) error {
	if m.CreateFunc != nil {
		return m.CreateFunc(ctx, l)
	}
	return nil
}
