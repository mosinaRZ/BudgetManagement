package mongodb

import (
	"context"
	"errors"
	"log/slog"
	"time"

	"go.mongodb.org/mongo-driver/bson"
	"go.mongodb.org/mongo-driver/bson/primitive"
	"go.mongodb.org/mongo-driver/mongo"
	"go.mongodb.org/mongo-driver/mongo/options"

	"github.com/mosinaRZ/finance-sync-backend/internal/domain/apperror"
	"github.com/mosinaRZ/finance-sync-backend/internal/domain/entity"
	"github.com/mosinaRZ/finance-sync-backend/internal/infrastructure/mongodb/models"
)

const usersCollectionName = "users"

type userRepositoryImpl struct{ collection *mongo.Collection }

func NewUserRepository(db *mongo.Database) *userRepositoryImpl {
	return &userRepositoryImpl{collection: db.Collection(usersCollectionName)}
}

func (r *userRepositoryImpl) Create(ctx context.Context, user *entity.User) error {
	ctx, cancel := context.WithTimeout(ctx, 10*time.Second)
	defer cancel()
	m := models.UserModelFromEntity(user)
	if m.ID.IsZero() {
		m.ID = primitive.NewObjectID()
	}
	_, err := r.collection.InsertOne(ctx, m)
	if err != nil {
		if mongo.IsDuplicateKeyError(err) {
			return apperror.ErrConflict("user already exists")
		}
		slog.Error("user create failed", "error", err)
		return apperror.ErrInternal("failed to create user")
	}
	user.ID = m.ID.Hex()
	return nil
}

func (r *userRepositoryImpl) FindByPhoneHash(ctx context.Context, phoneHash string) (*entity.User, error) {
	return r.findOne(ctx, bson.M{"phoneHash": phoneHash})
}

func (r *userRepositoryImpl) FindByID(ctx context.Context, id string) (*entity.User, error) {
	oid, err := primitive.ObjectIDFromHex(id)
	if err != nil {
		return nil, apperror.ErrNotFound("user not found")
	}
	return r.findOne(ctx, bson.M{"_id": oid})
}

func (r *userRepositoryImpl) findOne(ctx context.Context, filter interface{}) (*entity.User, error) {
	ctx, cancel := context.WithTimeout(ctx, 10*time.Second)
	defer cancel()
	var m models.UserModel
	if err := r.collection.FindOne(ctx, filter).Decode(&m); err != nil {
		if errors.Is(err, mongo.ErrNoDocuments) {
			return nil, apperror.ErrNotFound("user not found")
		}
		slog.Error("user lookup failed", "error", err)
		return nil, apperror.ErrInternal("failed to read user")
	}
	return m.ToEntity(), nil
}

func (r *userRepositoryImpl) Update(ctx context.Context, user *entity.User) error {
	oid, err := primitive.ObjectIDFromHex(user.ID)
	if err != nil {
		return apperror.ErrNotFound("user not found")
	}
	ctx, cancel := context.WithTimeout(ctx, 10*time.Second)
	defer cancel()
	m := models.UserModelFromEntity(user)
	result, err := r.collection.ReplaceOne(ctx, bson.M{"_id": oid}, m, options.Replace().SetUpsert(false))
	if err != nil {
		return apperror.ErrInternal("failed to update user", err)
	}
	if result.MatchedCount == 0 {
		return apperror.ErrNotFound("user not found")
	}
	return nil
}

func (r *userRepositoryImpl) AddDevice(ctx context.Context, userID, deviceID string) error {
	oid, err := primitive.ObjectIDFromHex(userID)
	if err != nil {
		return apperror.ErrNotFound("user not found")
	}
	ctx, cancel := context.WithTimeout(ctx, 10*time.Second)
	defer cancel()
	res, err := r.collection.UpdateOne(ctx, bson.M{"_id": oid}, bson.M{"$addToSet": bson.M{"devices": deviceID}, "$set": bson.M{"updatedAt": time.Now().UTC()}})
	if err != nil {
		return apperror.ErrInternal("failed to add device", err)
	}
	if res.MatchedCount == 0 {
		return apperror.ErrNotFound("user not found")
	}
	return nil
}
