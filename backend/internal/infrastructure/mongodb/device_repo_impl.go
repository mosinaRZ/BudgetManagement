package mongodb

import (
	"context"
	"time"

	"github.com/mosinaRZ/finance-sync-backend/internal/domain/apperror"
	"github.com/mosinaRZ/finance-sync-backend/internal/domain/entity"
	"go.mongodb.org/mongo-driver/bson"
	"go.mongodb.org/mongo-driver/bson/primitive"
	"go.mongodb.org/mongo-driver/mongo"
	"go.mongodb.org/mongo-driver/mongo/options"
)

const devicesCollectionName = "devices"

// maxListedDevices bounds the number of devices returned for one account.
const maxListedDevices = 100

type deviceRepo struct{ c *mongo.Collection }

func NewDeviceRepository(db *mongo.Database) *deviceRepo {
	return &deviceRepo{c: db.Collection(devicesCollectionName)}
}
func (r *deviceRepo) Register(ctx context.Context, d *entity.Device) error {
	set := bson.M{"lastSeenAt": d.LastSeenAt}
	// Descriptive metadata is only overwritten when the client actually supplied it,
	// so a sign-in from an older client can never erase details that are already known.
	for key, value := range map[string]string{
		"name":       d.Name,
		"model":      d.Model,
		"platform":   d.Platform,
		"osVersion":  d.OSVersion,
		"appVersion": d.AppVersion,
		"lastIp":     d.LastIP,
	} {
		if value != "" {
			set[key] = value
		}
	}
	_, err := r.c.UpdateOne(ctx, bson.M{"userId": d.UserID, "deviceId": d.ID}, bson.M{"$set": set, "$setOnInsert": bson.M{"userId": d.UserID, "deviceId": d.ID, "createdAt": d.CreatedAt}}, options.Update().SetUpsert(true))
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
	// Most recently active first; the cap keeps the response bounded for pathological accounts.
	cur, err := r.c.Find(ctx, bson.M{"userId": userID}, options.Find().SetSort(bson.D{{Key: "lastSeenAt", Value: -1}}).SetLimit(maxListedDevices))
	if err != nil {
		return nil, apperror.ErrInternal("failed to list devices", err)
	}
	defer cur.Close(ctx)
	var out []*entity.Device
	for cur.Next(ctx) {
		var m struct {
			ID         primitive.ObjectID `bson:"_id"`
			UserID     string             `bson:"userId"`
			DeviceID   string             `bson:"deviceId"`
			Name       string             `bson:"name"`
			Model      string             `bson:"model"`
			Platform   string             `bson:"platform"`
			OSVersion  string             `bson:"osVersion"`
			AppVersion string             `bson:"appVersion"`
			LastIP     string             `bson:"lastIp"`
			LastSeenAt time.Time          `bson:"lastSeenAt"`
			CreatedAt  time.Time          `bson:"createdAt"`
		}
		if err := cur.Decode(&m); err != nil {
			return nil, apperror.ErrInternal("failed to decode device", err)
		}
		out = append(out, &entity.Device{
			ID: m.DeviceID, UserID: m.UserID,
			Name: m.Name, Model: m.Model, Platform: m.Platform, OSVersion: m.OSVersion, AppVersion: m.AppVersion, LastIP: m.LastIP,
			LastSeenAt: m.LastSeenAt, CreatedAt: m.CreatedAt,
		})
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
