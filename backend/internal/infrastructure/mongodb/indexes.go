package mongodb

import (
	"context"
	"fmt"
	"go.mongodb.org/mongo-driver/bson"
	"go.mongodb.org/mongo-driver/mongo"
	"go.mongodb.org/mongo-driver/mongo/options"
	"time"
)

func EnsureIndexes(ctx context.Context, db *mongo.Database) error {
	ctx, cancel := context.WithTimeout(ctx, 30*time.Second)
	defer cancel()
	if _, err := db.Collection(syncRecordsCollectionName).Indexes().CreateMany(ctx, []mongo.IndexModel{
		{Keys: bson.D{{Key: "userId", Value: 1}, {Key: "entityType", Value: 1}, {Key: "entityId", Value: 1}}, Options: options.Index().SetUnique(true).SetName("uniq_sync_identity")},
		{Keys: bson.D{{Key: "userId", Value: 1}, {Key: "serverRevision", Value: 1}}, Options: options.Index().SetName("idx_sync_cursor")},
	}); err != nil {
		return fmt.Errorf("sync_records indexes: %w", err)
	}
	if _, err := db.Collection(syncOperationsCollectionName).Indexes().CreateOne(ctx, mongo.IndexModel{Keys: bson.D{{Key: "userId", Value: 1}, {Key: "requestId", Value: 1}}, Options: options.Index().SetUnique(true).SetName("uniq_sync_request")}); err != nil {
		return fmt.Errorf("sync_operations indexes: %w", err)
	}
	if _, err := db.Collection(syncRevisionCountersCollectionName).Indexes().CreateOne(ctx, mongo.IndexModel{Keys: bson.D{{Key: "_id", Value: 1}}, Options: options.Index().SetUnique(true).SetName("uniq_sync_revision_counter")}); err != nil {
		return fmt.Errorf("sync_revision_counters indexes: %w", err)
	}
	if _, err := db.Collection(usersCollectionName).Indexes().CreateOne(ctx, mongo.IndexModel{
		Keys:    bson.D{{Key: "phoneHash", Value: 1}},
		Options: options.Index().SetUnique(true).SetName("uniq_user_phone_hash"),
	}); err != nil {
		return fmt.Errorf("users indexes: %w", err)
	}
	if _, err := db.Collection(refreshTokensCollectionName).Indexes().CreateMany(ctx, []mongo.IndexModel{
		{Keys: bson.D{{Key: "tokenHash", Value: 1}}, Options: options.Index().SetUnique(true).SetName("uniq_refresh_token_hash")},
		{Keys: bson.D{{Key: "userId", Value: 1}, {Key: "revokedAt", Value: 1}}, Options: options.Index().SetName("idx_refresh_user_revoked")},
	}); err != nil {
		return fmt.Errorf("refresh_tokens indexes: %w", err)
	}
	return nil
}

// BackfillSyncRevisions assigns revisions to legacy sync records created before
// the revision cursor was introduced. It runs during startup before the HTTP
// server begins accepting requests.
func BackfillSyncRevisions(ctx context.Context, db *mongo.Database) error {
	ctx, cancel := context.WithTimeout(ctx, 2*time.Minute)
	defer cancel()

	cur, err := db.Collection(syncRecordsCollectionName).Find(ctx, bson.M{"serverRevision": bson.M{"$exists": false}}, options.Find().SetSort(bson.D{{Key: "updatedAt", Value: 1}, {Key: "_id", Value: 1}}))
	if err != nil {
		return fmt.Errorf("sync revision backfill query: %w", err)
	}
	defer cur.Close(ctx)

	for cur.Next(ctx) {
		var m struct {
			ID interface{} `bson:"_id"`
		}
		if err := cur.Decode(&m); err != nil {
			return fmt.Errorf("sync revision backfill decode: %w", err)
		}
		var counter struct {
			Value int64 `bson:"value"`
		}
		if err := db.Collection(syncRevisionCountersCollectionName).FindOneAndUpdate(ctx, bson.M{"_id": "global"}, bson.M{"$inc": bson.M{"value": int64(1)}}, options.FindOneAndUpdate().SetUpsert(true).SetReturnDocument(options.After)).Decode(&counter); err != nil {
			return fmt.Errorf("sync revision backfill counter: %w", err)
		}
		if _, err := db.Collection(syncRecordsCollectionName).UpdateOne(ctx, bson.M{"_id": m.ID, "serverRevision": bson.M{"$exists": false}}, bson.M{"$set": bson.M{"serverRevision": counter.Value}}); err != nil {
			return fmt.Errorf("sync revision backfill write: %w", err)
		}
	}
	if err := cur.Err(); err != nil {
		return fmt.Errorf("sync revision backfill cursor: %w", err)
	}
	return nil
}
