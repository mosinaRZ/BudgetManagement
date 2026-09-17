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
	if _, err := db.Collection(syncOperationsCollectionName).Indexes().CreateMany(ctx, []mongo.IndexModel{
		{Keys: bson.D{{Key: "userId", Value: 1}, {Key: "requestId", Value: 1}}, Options: options.Index().SetUnique(true).SetName("uniq_sync_request")},
		{Keys: bson.D{{Key: "createdAt", Value: 1}}, Options: options.Index().SetExpireAfterSeconds(7 * 24 * 3600).SetName("ttl_sync_operations")},
	}); err != nil {
		return fmt.Errorf("sync_operations indexes: %w", err)
	}
	if _, err := db.Collection(syncRevisionCountersCollectionName).Indexes().CreateOne(ctx, mongo.IndexModel{Keys: bson.D{{Key: "_id", Value: 1}}, Options: options.Index().SetName("uniq_sync_revision_counter")}); err != nil {
		return fmt.Errorf("sync_revision_counters indexes: %w", err)
	}
	if _, err := db.Collection(usersCollectionName).Indexes().CreateMany(ctx, []mongo.IndexModel{
		{Keys: bson.D{{Key: "phoneHash", Value: 1}}, Options: options.Index().SetUnique(true).SetSparse(true).SetName("uniq_user_phone_hash")},
		{Keys: bson.D{{Key: "emailHash", Value: 1}}, Options: options.Index().SetUnique(true).SetSparse(true).SetName("uniq_user_email_hash")},
	}); err != nil {
		return fmt.Errorf("users indexes: %w", err)
	}
	if _, err := db.Collection(refreshTokensCollectionName).Indexes().CreateMany(ctx, []mongo.IndexModel{
		{Keys: bson.D{{Key: "tokenHash", Value: 1}}, Options: options.Index().SetUnique(true).SetName("uniq_refresh_token_hash")},
		{Keys: bson.D{{Key: "userId", Value: 1}, {Key: "revokedAt", Value: 1}}, Options: options.Index().SetName("idx_refresh_user_revoked")},
		{Keys: bson.D{{Key: "expiresAt", Value: 1}}, Options: options.Index().SetExpireAfterSeconds(0).SetName("ttl_refresh_tokens")},
	}); err != nil {
		return fmt.Errorf("refresh_tokens indexes: %w", err)
	}
	if _, err := db.Collection(otpCollectionName).Indexes().CreateMany(ctx, []mongo.IndexModel{
		{Keys: bson.D{{Key: "expiresAt", Value: 1}}, Options: options.Index().SetExpireAfterSeconds(0).SetName("ttl_otp")},
		{Keys: bson.D{{Key: "destinationHash", Value: 1}, {Key: "channel", Value: 1}, {Key: "purpose", Value: 1}, {Key: "createdAt", Value: -1}}, Options: options.Index().SetName("idx_otp_rate")},
	}); err != nil {
		return fmt.Errorf("otp indexes: %w", err)
	}
	if _, err := db.Collection(auditLogsCollectionName).Indexes().CreateMany(ctx, []mongo.IndexModel{
		{Keys: bson.D{{Key: "targetUserId", Value: 1}, {Key: "createdAt", Value: -1}}, Options: options.Index().SetName("idx_audit_target_created")},
	}); err != nil {
		return fmt.Errorf("audit_logs indexes: %w", err)
	}
	if _, err := db.Collection(devicesCollectionName).Indexes().CreateMany(ctx, []mongo.IndexModel{
		{Keys: bson.D{{Key: "userId", Value: 1}, {Key: "deviceId", Value: 1}}, Options: options.Index().SetUnique(true).SetName("uniq_device")},
		{Keys: bson.D{{Key: "userId", Value: 1}, {Key: "lastSeenAt", Value: -1}}, Options: options.Index().SetName("idx_device_last_seen")},
	}); err != nil {
		return fmt.Errorf("devices indexes: %w", err)
	}
	return nil
}
func BackfillSyncRevisions(ctx context.Context, db *mongo.Database) error { return nil }
