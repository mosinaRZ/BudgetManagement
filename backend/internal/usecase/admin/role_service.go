package admin

import (
	"context"
	"time"

	"github.com/mosinaRZ/finance-sync-backend/internal/domain/apperror"
	"github.com/mosinaRZ/finance-sync-backend/internal/domain/entity"
	"github.com/mosinaRZ/finance-sync-backend/internal/domain/repository"
)

type RoleService interface {
	UpdateUserRole(context.Context, string, string, entity.Role) error
}

type Service struct {
	users repository.UserRepository
	audit repository.AuditLogRepository
}

func NewRoleService(users repository.UserRepository, audit ...repository.AuditLogRepository) *Service {
	var a repository.AuditLogRepository
	if len(audit) > 0 {
		a = audit[0]
	}
	return &Service{users: users, audit: a}
}

func (s *Service) UpdateUserRole(ctx context.Context, actorUserID, userID string, role entity.Role) error {
	if s == nil || s.users == nil {
		return apperror.ErrInternal("role service is not configured")
	}
	if actorUserID == "" || userID == "" {
		return apperror.ErrValidation("user id is required")
	}
	if !role.Valid() {
		return apperror.ErrValidation("invalid user role")
	}

	target, err := s.users.FindByID(ctx, userID)
	if err != nil {
		return err
	}
	oldRole := target.Role
	if err := s.users.UpdateRole(ctx, userID, role); err != nil {
		return err
	}
	if s.audit != nil {
		if err := s.audit.Create(ctx, &entity.AuditLog{
			ActorUserID: actorUserID, TargetUserID: userID, Action: "user_role_updated",
			OldValue: string(oldRole), NewValue: string(role), CreatedAt: time.Now().UTC(),
		}); err != nil {
			// Role changes remain successful if the deployment cannot provide transactional audit writes.
			return nil
		}
	}
	return nil
}
