package mongodb

import (
	"context"
	"time"

	"github.com/mosinaRZ/finance-sync-backend/internal/domain/apperror"
	"github.com/mosinaRZ/finance-sync-backend/internal/domain/entity"
	"go.mongodb.org/mongo-driver/bson"
	"go.mongodb.org/mongo-driver/bson/primitive"
	"go.mongodb.org/mongo-driver/mongo"
)

const auditLogsCollectionName = "audit_logs"

type auditLogRepo struct{ c *mongo.Collection }

func NewAuditLogRepository(db *mongo.Database) *auditLogRepo {
	return &auditLogRepo{c: db.Collection(auditLogsCollectionName)}
}

func (r *auditLogRepo) Create(ctx context.Context, l *entity.AuditLog) error {
	if l.CreatedAt.IsZero() {
		l.CreatedAt = time.Now().UTC()
	}
	doc := bson.M{"actorUserId": l.ActorUserID, "targetUserId": l.TargetUserID, "action": l.Action, "oldValue": l.OldValue, "newValue": l.NewValue, "createdAt": l.CreatedAt}
	res, err := r.c.InsertOne(ctx, doc)
	if err != nil {
		return apperror.ErrInternal("failed to create audit log", err)
	}
	if oid, ok := res.InsertedID.(primitive.ObjectID); ok {
		l.ID = oid.Hex()
	}
	return nil
}
