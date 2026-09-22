package admin

import (
	"context"
	"testing"

	"github.com/mosinaRZ/finance-sync-backend/internal/domain/entity"
	"github.com/mosinaRZ/finance-sync-backend/internal/usecase/mocks"
)

func TestUpdateUserRoleCreatesAuditLog(t *testing.T) {
	var got *entity.AuditLog
	users := &mocks.MockUserRepository{
		FindByIDFunc: func(context.Context, string) (*entity.User, error) {
			return &entity.User{ID: "u2", Role: entity.RoleUser}, nil
		},
		UpdateRoleFunc: func(context.Context, string, entity.Role) error { return nil },
	}
	audit := &mocks.MockAuditLogRepository{CreateFunc: func(_ context.Context, l *entity.AuditLog) error { got = l; return nil }}
	err := NewRoleService(users, audit).UpdateUserRole(context.Background(), "admin1", "u2", entity.RoleAdmin)
	if err != nil {
		t.Fatal(err)
	}
	if got == nil || got.ActorUserID != "admin1" || got.TargetUserID != "u2" || got.OldValue != string(entity.RoleUser) || got.NewValue != string(entity.RoleAdmin) {
		t.Fatalf("unexpected audit log: %+v", got)
	}
}

func TestUpdateUserRoleReportsAuditFailure(t *testing.T) {
	users := &mocks.MockUserRepository{
		FindByIDFunc: func(context.Context, string) (*entity.User, error) {
			return &entity.User{ID: "u2", Role: entity.RoleUser}, nil
		},
		UpdateRoleFunc: func(context.Context, string, entity.Role) error { return nil },
	}
	audit := &mocks.MockAuditLogRepository{CreateFunc: func(context.Context, *entity.AuditLog) error { return context.Canceled }}
	if err := NewRoleService(users, audit).UpdateUserRole(context.Background(), "admin1", "u2", entity.RoleAdmin); err == nil {
		t.Fatal("expected audit failure to be surfaced")
	}
}
