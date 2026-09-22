package mongodb

import (
	"context"
	"errors"
	"time"

	"github.com/mosinaRZ/finance-sync-backend/internal/domain/apperror"
	"github.com/mosinaRZ/finance-sync-backend/internal/domain/entity"
	"github.com/mosinaRZ/finance-sync-backend/internal/infrastructure/mongodb/models"
	"go.mongodb.org/mongo-driver/bson"
	"go.mongodb.org/mongo-driver/bson/primitive"
	"go.mongodb.org/mongo-driver/mongo"
	"go.mongodb.org/mongo-driver/mongo/options"
)

const usersCollectionName = "users"

// MaxUserDevices bounds per-user device fan-out and prevents unbounded document growth.
const MaxUserDevices = 20

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
func (r *userRepo) UpdateCredentials(ctx context.Context, userID, passwordHash, authSalt, kdfSalt string, passwordKeyEnvelope, passwordKeyNonce []byte) error {
	oid, err := primitive.ObjectIDFromHex(userID)
	if err != nil {
		return apperror.ErrNotFound("user not found")
	}
	res, err := r.c.UpdateOne(ctx, bson.M{"_id": oid}, bson.M{"$set": bson.M{
		"passwordHash":        passwordHash,
		"authSalt":            authSalt,
		"kdfSalt":             kdfSalt,
		"passwordKeyEnvelope": append([]byte(nil), passwordKeyEnvelope...),
		"passwordKeyNonce":    append([]byte(nil), passwordKeyNonce...),
		"failedLoginAttempts": 0,
		"updatedAt":           time.Now().UTC(),
	}, "$unset": bson.M{"lockedUntil": ""}})
	if err != nil {
		return apperror.ErrInternal("failed to update credentials", err)
	}
	if res.MatchedCount == 0 {
		return apperror.ErrNotFound("user not found")
	}
	return nil
}

func (r *userRepo) AddDevice(ctx context.Context, userID, deviceID string) error {
	oid, err := primitive.ObjectIDFromHex(userID)
	if err != nil {
		return apperror.ErrNotFound("user not found")
	}
	// Count-and-add is performed as a single pipeline update so the cap cannot be bypassed by concurrent logins.
	pipeline := mongo.Pipeline{
		{{Key: "$set", Value: bson.M{"_deviceCount": bson.M{"$size": bson.M{"$ifNull": bson.A{"$devices", bson.A{}}}}}}},
		{{Key: "$set", Value: bson.M{"devices": bson.M{"$cond": bson.A{
			bson.M{"$in": bson.A{deviceID, bson.M{"$ifNull": bson.A{"$devices", bson.A{}}}}},
			"$devices",
			bson.M{"$cond": bson.A{
				bson.M{"$gte": bson.A{"$_deviceCount", MaxUserDevices}},
				"$devices",
				bson.M{"$concatArrays": bson.A{bson.M{"$ifNull": bson.A{"$devices", bson.A{}}}, bson.A{deviceID}}},
			}},
		}}}}},
		{{Key: "$set", Value: bson.M{"updatedAt": time.Now().UTC()}}},
		{{Key: "$unset", Value: "_deviceCount"}},
	}
	res, err := r.c.UpdateOne(ctx, bson.M{"_id": oid}, pipeline)
	if err != nil {
		return apperror.ErrInternal("failed to add device", err)
	}
	if res.MatchedCount == 0 {
		return apperror.ErrNotFound("user not found")
	}
	// Re-check the resulting size to return the documented validation error at the cap.
	var current models.UserModel
	if err := r.c.FindOne(ctx, bson.M{"_id": oid}, options.FindOne().SetProjection(bson.M{"devices": 1})).Decode(&current); err != nil {
		return apperror.ErrInternal("failed to verify device limit", err)
	}
	if len(current.Devices) >= MaxUserDevices {
		for _, d := range current.Devices {
			if d == deviceID {
				return nil
			}
		}
		return apperror.ErrValidation("maximum number of devices reached")
	}
	return nil
}

func (r *userRepo) UpdateRole(ctx context.Context, userID string, role entity.Role) error {
	if !role.Valid() {
		return apperror.ErrValidation("invalid user role")
	}
	oid, err := primitive.ObjectIDFromHex(userID)
	if err != nil {
		return apperror.ErrNotFound("user not found")
	}
	res, err := r.c.UpdateOne(ctx, bson.M{"_id": oid}, bson.M{"$set": bson.M{"role": role, "updatedAt": time.Now().UTC()}, "$inc": bson.M{"sessionVersion": 1}})
	if err != nil {
		return apperror.ErrInternal("failed to update user role", err)
	}
	if res.MatchedCount == 0 {
		return apperror.ErrNotFound("user not found")
	}
	return nil
}

func (r *userRepo) RecordFailedLogin(ctx context.Context, userID string, now time.Time, threshold int, lockDuration time.Duration) error {
	// Atomic conditional update prevents concurrent login attempts from racing the lock threshold.
	oid, err := primitive.ObjectIDFromHex(userID)
	if err != nil {
		return apperror.ErrNotFound("user not found")
	}
	filter := bson.M{"_id": oid}
	update := bson.M{"$inc": bson.M{"failedLoginAttempts": 1}, "$set": bson.M{"updatedAt": now}}
	res, err := r.c.UpdateOne(ctx, filter, update)
	if err != nil {
		return apperror.ErrInternal("failed to record failed login", err)
	}
	if res.MatchedCount == 0 {
		return apperror.ErrNotFound("user not found")
	}

	// Set the first lock atomically once the threshold is reached. The exponential duration is bounded.
	var u models.UserModel
	if err := r.c.FindOne(ctx, filter, options.FindOne().SetProjection(bson.M{"failedLoginAttempts": 1, "lockedUntil": 1})).Decode(&u); err != nil {
		return apperror.ErrInternal("failed to read login state", err)
	}
	if u.FailedLoginAttempts >= threshold && (u.LockedUntil == nil || !u.LockedUntil.After(now)) {
		exponent := u.FailedLoginAttempts - threshold
		if exponent > 4 {
			exponent = 4
		}
		d := lockDuration * time.Duration(1<<exponent)
		_, err = r.c.UpdateOne(ctx, bson.M{"_id": oid, "$or": bson.A{
			bson.M{"lockedUntil": bson.M{"$exists": false}},
			bson.M{"lockedUntil": bson.M{"$lte": now}},
		}}, bson.M{"$set": bson.M{"lockedUntil": now.Add(d), "updatedAt": now}})
		if err != nil {
			return apperror.ErrInternal("failed to lock account", err)
		}
	}
	return nil
}

func (r *userRepo) ResetFailedLogin(ctx context.Context, userID string) error {
	oid, err := primitive.ObjectIDFromHex(userID)
	if err != nil {
		return apperror.ErrNotFound("user not found")
	}
	_, err = r.c.UpdateOne(ctx, bson.M{"_id": oid}, bson.M{"$set": bson.M{"failedLoginAttempts": 0, "updatedAt": time.Now().UTC()}, "$unset": bson.M{"lockedUntil": ""}})
	if err != nil {
		return apperror.ErrInternal("failed to reset login state", err)
	}
	return nil
}

func (r *userRepo) GetSessionVersion(ctx context.Context, userID string) (uint64, error) {
	oid, err := primitive.ObjectIDFromHex(userID)
	if err != nil {
		return 0, apperror.ErrNotFound("user not found")
	}
	var m models.UserModel
	if err := r.c.FindOne(ctx, bson.M{"_id": oid}, options.FindOne().SetProjection(bson.M{"sessionVersion": 1})).Decode(&m); err != nil {
		if errors.Is(err, mongo.ErrNoDocuments) {
			return 0, apperror.ErrNotFound("user not found")
		}
		return 0, apperror.ErrInternal("failed to read session version", err)
	}
	return m.SessionVersion, nil
}

func (r *userRepo) IncrementSessionVersion(ctx context.Context, userID string) (uint64, error) {
	oid, err := primitive.ObjectIDFromHex(userID)
	if err != nil {
		return 0, apperror.ErrNotFound("user not found")
	}
	var m models.UserModel
	err = r.c.FindOneAndUpdate(ctx, bson.M{"_id": oid}, bson.M{"$inc": bson.M{"sessionVersion": 1}, "$set": bson.M{"updatedAt": time.Now().UTC()}}, options.FindOneAndUpdate().SetReturnDocument(options.After).SetProjection(bson.M{"sessionVersion": 1})).Decode(&m)
	if errors.Is(err, mongo.ErrNoDocuments) {
		return 0, apperror.ErrNotFound("user not found")
	}
	if err != nil {
		return 0, apperror.ErrInternal("failed to revoke access sessions", err)
	}
	return m.SessionVersion, nil
}
