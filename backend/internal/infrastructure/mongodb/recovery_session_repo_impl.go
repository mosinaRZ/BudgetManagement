package mongodb

import (
	"context"
	"errors"
	"time"

	"go.mongodb.org/mongo-driver/bson"
	"go.mongodb.org/mongo-driver/bson/primitive"
	"go.mongodb.org/mongo-driver/mongo"

	"github.com/mosinaRZ/finance-sync-backend/internal/domain/apperror"
	"github.com/mosinaRZ/finance-sync-backend/internal/domain/entity"
)

const recoverySessionsCollectionName = "recovery_sessions"

type recoverySessionRepo struct{ c *mongo.Collection }

func NewRecoverySessionRepository(db *mongo.Database) *recoverySessionRepo {
	return &recoverySessionRepo{c: db.Collection(recoverySessionsCollectionName)}
}

func (r *recoverySessionRepo) Create(ctx context.Context, s *entity.RecoverySession) error {
	if s == nil || s.TokenHash == "" || s.UserID == "" || s.ExpiresAt.IsZero() {
		return apperror.ErrValidation("invalid recovery session")
	}
	doc := bson.M{
		"userId": s.UserID, "tokenHash": s.TokenHash,
		"createdAt": s.CreatedAt, "expiresAt": s.ExpiresAt,
	}
	if s.ID != "" {
		id, err := primitive.ObjectIDFromHex(s.ID)
		if err != nil {
			return apperror.ErrValidation("invalid recovery session id")
		}
		doc["_id"] = id
	}
	res, err := r.c.InsertOne(ctx, doc)
	if err != nil {
		if mongo.IsDuplicateKeyError(err) {
			return apperror.ErrConflict("recovery session already exists")
		}
		return apperror.ErrInternal("failed to create recovery session", err)
	}
	if oid, ok := res.InsertedID.(primitive.ObjectID); ok {
		s.ID = oid.Hex()
	}
	return nil
}

func (r *recoverySessionRepo) Consume(ctx context.Context, tokenHash string, now time.Time) (*entity.RecoverySession, error) {
	var doc struct {
		ID        primitive.ObjectID `bson:"_id"`
		UserID    string             `bson:"userId"`
		TokenHash string             `bson:"tokenHash"`
		CreatedAt time.Time          `bson:"createdAt"`
		ExpiresAt time.Time          `bson:"expiresAt"`
		UsedAt    *time.Time         `bson:"usedAt,omitempty"`
	}
	filter := bson.M{"tokenHash": tokenHash, "usedAt": bson.M{"$exists": false}, "expiresAt": bson.M{"$gt": now}}
	updated := now
	if err := r.c.FindOneAndUpdate(ctx, filter, bson.M{"$set": bson.M{"usedAt": updated}}, nil).Decode(&doc); err != nil {
		if errors.Is(err, mongo.ErrNoDocuments) {
			return nil, apperror.ErrUnauthorized("invalid or expired recovery session")
		}
		return nil, apperror.ErrInternal("failed to consume recovery session", err)
	}
	return &entity.RecoverySession{ID: doc.ID.Hex(), UserID: doc.UserID, TokenHash: doc.TokenHash, CreatedAt: doc.CreatedAt, ExpiresAt: doc.ExpiresAt, UsedAt: doc.UsedAt}, nil
}
