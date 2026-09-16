package mongodb

import (
	"context"
	"github.com/mosinaRZ/finance-sync-backend/internal/domain/apperror"
	"github.com/mosinaRZ/finance-sync-backend/internal/domain/entity"
	"go.mongodb.org/mongo-driver/bson"
	"go.mongodb.org/mongo-driver/bson/primitive"
	"go.mongodb.org/mongo-driver/mongo"
	"go.mongodb.org/mongo-driver/mongo/options"
	"time"
)

const devicesCollectionName = "devices"

type deviceRepo struct{ c *mongo.Collection }

func NewDeviceRepository(db *mongo.Database) *deviceRepo {
	return &deviceRepo{c: db.Collection(devicesCollectionName)}
}
func (r *deviceRepo) Register(ctx context.Context, d *entity.Device) error {
	_, err := r.c.UpdateOne(ctx, bson.M{"userId": d.UserID, "deviceId": d.ID}, bson.M{"$set": bson.M{"lastSeenAt": d.LastSeenAt}, "$setOnInsert": bson.M{"userId": d.UserID, "deviceId": d.ID, "createdAt": d.CreatedAt}}, options.Update().SetUpsert(true))
	if err != nil {
		return apperror.ErrInternal("failed to register device", err)
	}
	return nil
}
func (r *deviceRepo) Exists(ctx context.Context, userID, deviceID string) (bool, error) {
	var m struct {
		ID primitive.ObjectID `bson:"_id"`
	}
	err := r.c.FindOne(ctx, bson.M{"userId": userID, "deviceId": deviceID}).Decode(&m)
	if err == mongo.ErrNoDocuments {
		return false, nil
	}
	if err != nil {
		return false, apperror.ErrInternal("failed to verify device", err)
	}
	return true, nil
}
func (r *deviceRepo) List(ctx context.Context, userID string) ([]*entity.Device, error) {
	cur, err := r.c.Find(ctx, bson.M{"userId": userID})
	if err != nil {
		return nil, apperror.ErrInternal("failed to list devices", err)
	}
	defer cur.Close(ctx)
	var out []*entity.Device
	for cur.Next(ctx) {
		var m struct {
			ID                    primitive.ObjectID `bson:"_id"`
			UserID, DeviceID      string
			LastSeenAt, CreatedAt time.Time
		}
		if err := cur.Decode(&m); err != nil {
			return nil, apperror.ErrInternal("failed to decode device", err)
		}
		out = append(out, &entity.Device{ID: m.DeviceID, UserID: m.UserID, LastSeenAt: m.LastSeenAt, CreatedAt: m.CreatedAt})
	}
	return out, cur.Err()
}
func (r *deviceRepo) Revoke(ctx context.Context, userID, deviceID string) error {
	res, err := r.c.DeleteOne(ctx, bson.M{"userId": userID, "deviceId": deviceID})
	if err != nil {
		return apperror.ErrInternal("failed to revoke device", err)
	}
	if res.DeletedCount == 0 {
		return apperror.ErrNotFound("device not found")
	}
	return nil
}
func (r *deviceRepo) Touch(ctx context.Context, userID, deviceID string) error {
	_, err := r.c.UpdateOne(ctx, bson.M{"userId": userID, "deviceId": deviceID}, bson.M{"$set": bson.M{"lastSeenAt": time.Now().UTC()}})
	if err != nil {
		return apperror.ErrInternal("failed to update device", err)
	}
	return nil
}
