package repository

import (
	"context"
	"github.com/mosinaRZ/finance-sync-backend/internal/domain/entity"
)

type AuditLogRepository interface {
	Create(context.Context, *entity.AuditLog) error
}
