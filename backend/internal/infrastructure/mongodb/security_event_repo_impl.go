package mongodb

import (
	"context"
	"time"

	"github.com/mosinaRZ/finance-sync-backend/internal/domain/apperror"
	"github.com/mosinaRZ/finance-sync-backend/internal/domain/entity"
	"github.com/mosinaRZ/finance-sync-backend/internal/domain/repository"
	"go.mongodb.org/mongo-driver/bson"
	"go.mongodb.org/mongo-driver/bson/primitive"
	"go.mongodb.org/mongo-driver/mongo"
	"go.mongodb.org/mongo-driver/mongo/options"
)

const securityEventsCollectionName = "security_events"

type securityEventDoc struct {
	ID          primitive.ObjectID `bson:"_id,omitempty"`
	Type        string             `bson:"type"`
	Severity    string             `bson:"severity"`
	UserID      string             `bson:"userId,omitempty"`
	ActorUserID string             `bson:"actorUserId,omitempty"`
	DeviceID    string             `bson:"deviceId,omitempty"`
	DeviceName  string             `bson:"deviceName,omitempty"`
	Platform    string             `bson:"platform,omitempty"`
	AppVersion  string             `bson:"appVersion,omitempty"`
	IP          string             `bson:"ip,omitempty"`
	Detail      string             `bson:"detail,omitempty"`
	CreatedAt   time.Time          `bson:"createdAt"`
}

func (d securityEventDoc) toEntity() *entity.SecurityEvent {
	return &entity.SecurityEvent{
		ID: d.ID.Hex(), Type: entity.SecurityEventType(d.Type), UserID: d.UserID, ActorUserID: d.ActorUserID,
		DeviceID: d.DeviceID, DeviceName: d.DeviceName, Platform: d.Platform, AppVersion: d.AppVersion,
		IP: d.IP, Detail: d.Detail, CreatedAt: d.CreatedAt,
	}
}

type securityEventRepo struct{ c *mongo.Collection }

func NewSecurityEventRepository(db *mongo.Database) *securityEventRepo {
	return &securityEventRepo{c: db.Collection(securityEventsCollectionName)}
}

func (r *securityEventRepo) Record(ctx context.Context, e *entity.SecurityEvent) error {
	if e == nil {
		return nil
	}
	if e.CreatedAt.IsZero() {
		e.CreatedAt = time.Now().UTC()
	}
	doc := securityEventDoc{
		Type: string(e.Type), Severity: entity.SeverityOf(e.Type), UserID: e.UserID, ActorUserID: e.ActorUserID,
		DeviceID: e.DeviceID, DeviceName: e.DeviceName, Platform: e.Platform, AppVersion: e.AppVersion,
		IP: e.IP, Detail: e.Detail, CreatedAt: e.CreatedAt,
	}
	res, err := r.c.InsertOne(ctx, doc)
	if err != nil {
		return apperror.ErrInternal("failed to record security event", err)
	}
	if oid, ok := res.InsertedID.(primitive.ObjectID); ok {
		e.ID = oid.Hex()
	}
	return nil
}

func securityEventQuery(f repository.SecurityEventFilter) bson.M {
	q := bson.M{}
	if len(f.Types) > 0 {
		q["type"] = bson.M{"$in": f.Types}
	}
	if f.Severity != "" {
		q["severity"] = f.Severity
	}
	if f.UserID != "" {
		q["userId"] = f.UserID
	}
	rng := bson.M{}
	if !f.From.IsZero() {
		rng["$gte"] = f.From
	}
	if !f.To.IsZero() {
		rng["$lt"] = f.To
	}
	if len(rng) > 0 {
		q["createdAt"] = rng
	}
	return q
}

func (r *securityEventRepo) List(ctx context.Context, f repository.SecurityEventFilter) ([]*entity.SecurityEvent, int64, error) {
	q := securityEventQuery(f)
	total, err := r.c.CountDocuments(ctx, q)
	if err != nil {
		return nil, 0, apperror.ErrInternal("failed to count security events", err)
	}
	limit := int64(f.Limit)
	if limit <= 0 || limit > 200 {
		limit = 50
	}
	opts := options.Find().SetSort(bson.D{{Key: "createdAt", Value: -1}, {Key: "_id", Value: -1}}).SetLimit(limit)
	if f.Offset > 0 {
		opts.SetSkip(int64(f.Offset))
	}
	cur, err := r.c.Find(ctx, q, opts)
	if err != nil {
		return nil, 0, apperror.ErrInternal("failed to list security events", err)
	}
	defer cur.Close(ctx)
	out := make([]*entity.SecurityEvent, 0, limit)
	for cur.Next(ctx) {
		var d securityEventDoc
		if err := cur.Decode(&d); err != nil {
			return nil, 0, apperror.ErrInternal("failed to decode security event", err)
		}
		out = append(out, d.toEntity())
	}
	if err := cur.Err(); err != nil {
		return nil, 0, apperror.ErrInternal("failed to read security events", err)
	}
	return out, total, nil
}

func (r *securityEventRepo) Between(ctx context.Context, from, to time.Time, f repository.SecurityEventFilter, max int) ([]*entity.SecurityEvent, error) {
	f.From, f.To = from, to
	if max <= 0 {
		max = 50000
	}
	cur, err := r.c.Find(ctx, securityEventQuery(f), options.Find().SetSort(bson.D{{Key: "createdAt", Value: -1}}).SetLimit(int64(max)))
	if err != nil {
		return nil, apperror.ErrInternal("failed to query security events", err)
	}
	defer cur.Close(ctx)
	var out []*entity.SecurityEvent
	for cur.Next(ctx) {
		var d securityEventDoc
		if err := cur.Decode(&d); err != nil {
			return nil, apperror.ErrInternal("failed to decode security event", err)
		}
		out = append(out, d.toEntity())
	}
	if err := cur.Err(); err != nil {
		return nil, apperror.ErrInternal("failed to read security events", err)
	}
	return out, nil
}
