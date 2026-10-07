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

const deviceEventsCollectionName = "device_events"

// deviceEventTTL bounds how long device activity is kept. Clients ask for new events every
// few minutes, so a month is far more than any device needs to catch up after being offline.
const deviceEventTTL = 30 * 24 * time.Hour

type deviceEventRepo struct{ c *mongo.Collection }

func NewDeviceEventRepository(db *mongo.Database) *deviceEventRepo {
	return &deviceEventRepo{c: db.Collection(deviceEventsCollectionName)}
}

type deviceEventModel struct {
	ID            primitive.ObjectID `bson:"_id"`
	UserID        string             `bson:"userId"`
	Type          string             `bson:"type"`
	DeviceID      string             `bson:"deviceId"`
	DeviceName    string             `bson:"deviceName"`
	Platform      string             `bson:"platform"`
	ActorDeviceID string             `bson:"actorDeviceId"`
	ActorName     string             `bson:"actorName"`
	CreatedAt     time.Time          `bson:"createdAt"`
}

func (m deviceEventModel) toEntity() *entity.DeviceEvent {
	return &entity.DeviceEvent{
		ID: m.ID.Hex(), UserID: m.UserID, Type: entity.DeviceEventType(m.Type),
		DeviceID: m.DeviceID, DeviceName: m.DeviceName, Platform: m.Platform,
		ActorDeviceID: m.ActorDeviceID, ActorName: m.ActorName, CreatedAt: m.CreatedAt,
	}
}

func (r *deviceEventRepo) Record(ctx context.Context, e *entity.DeviceEvent) error {
	if e == nil {
		return nil
	}
	createdAt := e.CreatedAt
	if createdAt.IsZero() {
		createdAt = time.Now().UTC()
	}
	_, err := r.c.InsertOne(ctx, deviceEventModel{
		ID: primitive.NewObjectID(), UserID: e.UserID, Type: string(e.Type),
		DeviceID: e.DeviceID, DeviceName: e.DeviceName, Platform: e.Platform,
		ActorDeviceID: e.ActorDeviceID, ActorName: e.ActorName, CreatedAt: createdAt,
	})
	if err != nil {
		return apperror.ErrInternal("failed to record device event", err)
	}
	return nil
}

func (r *deviceEventRepo) ListAfter(ctx context.Context, userID string, after time.Time, limit int) ([]*entity.DeviceEvent, error) {
	if limit <= 0 {
		return nil, nil
	}
	cur, err := r.c.Find(ctx,
		bson.M{"userId": userID, "createdAt": bson.M{"$gt": after}},
		options.Find().SetSort(bson.D{{Key: "createdAt", Value: 1}, {Key: "_id", Value: 1}}).SetLimit(int64(limit)),
	)
	if err != nil {
		return nil, apperror.ErrInternal("failed to list device events", err)
	}
	defer cur.Close(ctx)
	var out []*entity.DeviceEvent
	for cur.Next(ctx) {
		var m deviceEventModel
		if err := cur.Decode(&m); err != nil {
			return nil, apperror.ErrInternal("failed to decode device event", err)
		}
		out = append(out, m.toEntity())
	}
	if err := cur.Err(); err != nil {
		return nil, apperror.ErrInternal("failed to list device events", err)
	}
	return out, nil
}
