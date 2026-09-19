package mongodb

import (
	"context"
	"errors"
	"github.com/mosinaRZ/finance-sync-backend/internal/domain/apperror"
	"github.com/mosinaRZ/finance-sync-backend/internal/domain/entity"
	"go.mongodb.org/mongo-driver/bson"
	"go.mongodb.org/mongo-driver/bson/primitive"
	"go.mongodb.org/mongo-driver/mongo"
	"time"
)

const otpCollectionName = "otp_challenges"

type otpRepo struct{ c *mongo.Collection }

func NewOTPRepository(db *mongo.Database) *otpRepo {
	return &otpRepo{c: db.Collection(otpCollectionName)}
}
func (r *otpRepo) Create(ctx context.Context, o *entity.OTPChallenge) error {
	if o == nil {
		return apperror.ErrValidation("OTP challenge is required")
	}

	var id primitive.ObjectID
	var err error
	if o.ID == "" {
		id = primitive.NewObjectID()
	} else {
		id, err = primitive.ObjectIDFromHex(o.ID)
		if err != nil {
			return apperror.ErrValidation("invalid OTP challenge id")
		}
	}

	m := bson.M{
		"_id":             id,
		"userId":          o.UserID,
		"destinationHash": o.DestinationHash,
		"channel":         o.Channel,
		"purpose":         string(o.Purpose),
		"codeHash":        o.CodeHash,
		"createdAt":       o.CreatedAt,
		"expiresAt":       o.ExpiresAt,
		"attempts":        o.Attempts,
	}
	if _, err := r.c.InsertOne(ctx, m); err != nil {
		if mongo.IsDuplicateKeyError(err) {
			return apperror.ErrConflict("OTP challenge already exists")
		}
		return apperror.ErrInternal("failed to create OTP", err)
	}
	o.ID = id.Hex()
	return nil
}
func (r *otpRepo) FindActive(ctx context.Context, id string) (*entity.OTPChallenge, error) {
	oid, err := primitive.ObjectIDFromHex(id)
	if err != nil {
		return nil, apperror.ErrNotFound("OTP challenge not found")
	}
	var m struct {
		ID              primitive.ObjectID `bson:"_id"`
		UserID          string             `bson:"userId"`
		DestinationHash string             `bson:"destinationHash"`
		Channel         string             `bson:"channel"`
		CodeHash        string             `bson:"codeHash"`
		Purpose         string             `bson:"purpose"`
		CreatedAt       time.Time          `bson:"createdAt"`
		ExpiresAt       time.Time          `bson:"expiresAt"`
		ConsumedAt      *time.Time         `bson:"consumedAt"`
		Attempts        int                `bson:"attempts"`
	}
	err = r.c.FindOne(ctx, bson.M{"_id": oid}).Decode(&m)
	if errors.Is(err, mongo.ErrNoDocuments) {
		return nil, apperror.ErrNotFound("OTP challenge not found")
	}
	if err != nil {
		return nil, apperror.ErrInternal("failed to read OTP", err)
	}
	return &entity.OTPChallenge{ID: m.ID.Hex(), UserID: m.UserID, DestinationHash: m.DestinationHash, Channel: m.Channel, CodeHash: m.CodeHash, Purpose: entity.OTPPurpose(m.Purpose), CreatedAt: m.CreatedAt, ExpiresAt: m.ExpiresAt, ConsumedAt: m.ConsumedAt, Attempts: m.Attempts}, nil
}
func (r *otpRepo) Consume(ctx context.Context, id string) error {
	oid, err := primitive.ObjectIDFromHex(id)
	if err != nil {
		return apperror.ErrNotFound("OTP challenge not found")
	}
	now := time.Now().UTC()
	res, err := r.c.UpdateOne(ctx, bson.M{"_id": oid, "consumedAt": bson.M{"$exists": false}}, bson.M{"$set": bson.M{"consumedAt": now}})
	if err != nil {
		return apperror.ErrInternal("failed to consume OTP", err)
	}
	if res.ModifiedCount == 0 {
		return apperror.ErrConflict("OTP already consumed")
	}
	return nil
}
func (r *otpRepo) IncrementAttempts(ctx context.Context, id string) error {
	oid, err := primitive.ObjectIDFromHex(id)
	if err != nil {
		return apperror.ErrNotFound("OTP challenge not found")
	}
	_, err = r.c.UpdateOne(ctx, bson.M{"_id": oid}, bson.M{"$inc": bson.M{"attempts": 1}})
	if err != nil {
		return apperror.ErrInternal("failed to update OTP attempts", err)
	}
	return nil
}
func (r *otpRepo) RecentCount(ctx context.Context, destination, channel string, purpose entity.OTPPurpose, minutes int) (int, error) {
	since := time.Now().UTC().Add(-time.Duration(minutes) * time.Minute)
	n, err := r.c.CountDocuments(ctx, bson.M{"destinationHash": destination, "channel": channel, "purpose": string(purpose), "createdAt": bson.M{"$gte": since}})
	if err != nil {
		return 0, apperror.ErrInternal("failed to check OTP rate", err)
	}
	return int(n), nil
}
