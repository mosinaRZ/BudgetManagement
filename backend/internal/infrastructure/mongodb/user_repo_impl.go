package mongodb

import (
	"context"
	"errors"
	"github.com/mosinaRZ/finance-sync-backend/internal/domain/apperror"
	"github.com/mosinaRZ/finance-sync-backend/internal/domain/entity"
	"github.com/mosinaRZ/finance-sync-backend/internal/infrastructure/mongodb/models"
	"go.mongodb.org/mongo-driver/bson"
	"go.mongodb.org/mongo-driver/bson/primitive"
	"go.mongodb.org/mongo-driver/mongo"
	"go.mongodb.org/mongo-driver/mongo/options"
	"time"
)

const usersCollectionName = "users"

type userRepo struct{ c *mongo.Collection }

func NewUserRepository(db *mongo.Database) *userRepo {
	return &userRepo{c: db.Collection(usersCollectionName)}
}
func (r *userRepo) Create(ctx context.Context, u *entity.User) error {
	m := models.UserModelFromEntity(u)
	if m.ID.IsZero() {
		m.ID = primitive.NewObjectID()
	}
	_, err := r.c.InsertOne(ctx, m)
	if mongo.IsDuplicateKeyError(err) {
		return apperror.ErrConflict("user already exists")
	}
	if err != nil {
		return apperror.ErrInternal("failed to create user", err)
	}
	u.ID = m.ID.Hex()
	return nil
}
func (r *userRepo) FindByPhoneHash(ctx context.Context, h string) (*entity.User, error) {
	return r.find(ctx, bson.M{"phoneHash": h})
}
func (r *userRepo) FindByEmailHash(ctx context.Context, h string) (*entity.User, error) {
	return r.find(ctx, bson.M{"emailHash": h})
}
func (r *userRepo) find(ctx context.Context, f bson.M) (*entity.User, error) {
	var m models.UserModel
	if err := r.c.FindOne(ctx, f).Decode(&m); err != nil {
		if errors.Is(err, mongo.ErrNoDocuments) {
			return nil, apperror.ErrNotFound("user not found")
		}
		return nil, apperror.ErrInternal("failed to read user", err)
	}
	return m.ToEntity(), nil
}
func (r *userRepo) FindByID(ctx context.Context, id string) (*entity.User, error) {
	oid, err := primitive.ObjectIDFromHex(id)
	if err != nil {
		return nil, apperror.ErrNotFound("user not found")
	}
	return r.find(ctx, bson.M{"_id": oid})
}
func (r *userRepo) Update(ctx context.Context, u *entity.User) error {
	oid, err := primitive.ObjectIDFromHex(u.ID)
	if err != nil {
		return apperror.ErrValidation("invalid user id")
	}
	u.UpdatedAt = time.Now().UTC()
	m := models.UserModelFromEntity(u)
	_, err = r.c.ReplaceOne(ctx, bson.M{"_id": oid}, m)
	if err != nil {
		return apperror.ErrInternal("failed to update user", err)
	}
	return nil
}
func (r *userRepo) AddDevice(ctx context.Context, userID, deviceID string) error {
	oid, err := primitive.ObjectIDFromHex(userID)
	if err != nil {
		return apperror.ErrNotFound("user not found")
	}
	res, err := r.c.UpdateOne(ctx, bson.M{"_id": oid}, bson.M{"$addToSet": bson.M{"devices": deviceID}, "$set": bson.M{"updatedAt": time.Now().UTC()}})
	if err != nil {
		return apperror.ErrInternal("failed to add device", err)
	}
	if res.MatchedCount == 0 {
		return apperror.ErrNotFound("user not found")
	}
	return nil
}

var _ = options.After
