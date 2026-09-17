package admin

import (
	"context"

	"github.com/mosinaRZ/finance-sync-backend/internal/domain/apperror"
	"github.com/mosinaRZ/finance-sync-backend/internal/domain/entity"
	"github.com/mosinaRZ/finance-sync-backend/internal/domain/repository"
)

type RoleService interface {
	UpdateUserRole(context.Context, string, entity.Role) error
}

type Service struct {
	users repository.UserRepository
}

func NewRoleService(users repository.UserRepository) *Service {
	return &Service{users: users}
}

func (s *Service) UpdateUserRole(ctx context.Context, userID string, role entity.Role) error {
	if s == nil || s.users == nil {
		return apperror.ErrInternal("role service is not configured")
	}
	if userID == "" {
		return apperror.ErrValidation("user id is required")
	}
	if !role.Valid() {
		return apperror.ErrValidation("invalid user role")
	}
	return s.users.UpdateRole(ctx, userID, role)
}
