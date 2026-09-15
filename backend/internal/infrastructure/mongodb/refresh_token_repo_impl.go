package mongodb

import (
	"context"
	"errors"
	"log/slog"
	"time"

	"go.mongodb.org/mongo-driver/bson"
	"go.mongodb.org/mongo-driver/bson/primitive"
	"go.mongodb.org/mongo-driver/mongo"

	"github.com/mosinaRZ/finance-sync-backend/internal/domain/apperror"
	"github.com/mosinaRZ/finance-sync-backend/internal/domain/entity"
	"github.com/mosinaRZ/finance-sync-backend/internal/infrastructure/mongodb/models"
)

const refreshTokensCollectionName = "refresh_tokens"

type refreshTokenRepositoryImpl struct{ collection *mongo.Collection }

func NewRefreshTokenRepository(db *mongo.Database) *refreshTokenRepositoryImpl {
	return &refreshTokenRepositoryImpl{collection: db.Collection(refreshTokensCollectionName)}
}

func (r *refreshTokenRepositoryImpl) Create(ctx context.Context, token *entity.RefreshToken) error {
	ctx, cancel := context.WithTimeout(ctx, 10*time.Second)
	defer cancel()
	m := models.RefreshTokenModelFromEntity(token)
	if m.ID.IsZero() {
		m.ID = primitive.NewObjectID()
	}
	_, err := r.collection.InsertOne(ctx, m)
	if err != nil {
		if mongo.IsDuplicateKeyError(err) {
			return apperror.ErrConflict("refresh token already exists")
		}
		slog.Error("refresh token create failed", "error", err)
		return apperror.ErrInternal("failed to create refresh token")
	}
	token.ID = m.ID.Hex()
	return nil
}

func (r *refreshTokenRepositoryImpl) FindByHash(ctx context.Context, hash string) (*entity.RefreshToken, error) {
	ctx, cancel := context.WithTimeout(ctx, 10*time.Second)
	defer cancel()
	var m models.RefreshTokenModel
	if err := r.collection.FindOne(ctx, bson.M{"tokenHash": hash}).Decode(&m); err != nil {
		if errors.Is(err, mongo.ErrNoDocuments) {
			return nil, apperror.ErrNotFound("refresh token not found")
		}
		return nil, apperror.ErrInternal("failed to read refresh token", err)
	}
	return m.ToEntity(), nil
}

func (r *refreshTokenRepositoryImpl) Revoke(ctx context.Context, hash string) error {
	ctx, cancel := context.WithTimeout(ctx, 10*time.Second)
	defer cancel()
	now := time.Now().UTC()
	res, err := r.collection.UpdateOne(ctx, bson.M{"tokenHash": hash, "revokedAt": bson.M{"$exists": false}}, bson.M{"$set": bson.M{"revokedAt": now}})
	if err != nil {
		return apperror.ErrInternal("failed to revoke refresh token", err)
	}
	if res.ModifiedCount == 0 {
		var existing models.RefreshTokenModel
		err = r.collection.FindOne(ctx, bson.M{"tokenHash": hash}).Decode(&existing)
		if errors.Is(err, mongo.ErrNoDocuments) {
			return apperror.ErrNotFound("refresh token not found")
		}
		if err != nil {
			return apperror.ErrInternal("failed to verify refresh token", err)
		}
		return apperror.ErrConflict("refresh token already revoked")
	}
	return nil
}

func (r *refreshTokenRepositoryImpl) RevokeAllForUser(ctx context.Context, userID string) error {
	ctx, cancel := context.WithTimeout(ctx, 10*time.Second)
	defer cancel()
	_, err := r.collection.UpdateMany(ctx, bson.M{"userId": userID, "revokedAt": bson.M{"$exists": false}}, bson.M{"$set": bson.M{"revokedAt": time.Now().UTC()}})
	if err != nil {
		return apperror.ErrInternal("failed to revoke user refresh tokens", err)
	}
	return nil
}
