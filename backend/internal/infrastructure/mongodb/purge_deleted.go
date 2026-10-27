package mongodb

import (
	"context"
	"fmt"
	"time"

	"go.mongodb.org/mongo-driver/bson"
	"go.mongodb.org/mongo-driver/bson/primitive"
	"go.mongodb.org/mongo-driver/mongo"
	"go.mongodb.org/mongo-driver/mongo/options"
)

// PurgeReport counts what a purge removed (or, in a dry run, would remove).
type PurgeReport struct {
	Users             int64
	SyncRecords       int64
	SyncOperations    int64
	SyncRevisionState int64
	RefreshTokens     int64
	RecoverySessions  int64
	Devices           int64
	DeviceEvents      int64
}

// PurgeDeletedUsers permanently removes soft-deleted accounts whose deletion happened at or before
// cutoff, together with everything stored for them (encrypted sync data, sessions, devices).
//
// This is the second half of the account-deletion policy: isDeleted=true hides and locks the
// account immediately and keeps it restorable during the grace period; once that period is over
// this function erases the data for good, which is what store data-deletion policies require.
//
// Child data is removed before the user document, so an interrupted run leaves the user flagged
// and the next run finishes the job. audit_logs are intentionally kept: they only reference opaque
// user ids and are needed to investigate abuse.
func PurgeDeletedUsers(ctx context.Context, db *mongo.Database, cutoff time.Time, dryRun bool) (PurgeReport, error) {
	var report PurgeReport
	users := db.Collection(usersCollectionName)
	cur, err := users.Find(ctx,
		bson.M{"isDeleted": true, "deletedAt": bson.M{"$lte": cutoff.UTC()}},
		options.Find().SetProjection(bson.M{"_id": 1}))
	if err != nil {
		return report, fmt.Errorf("find deleted users: %w", err)
	}
	var ids []primitive.ObjectID
	for cur.Next(ctx) {
		var doc struct {
			ID primitive.ObjectID `bson:"_id"`
		}
		if err := cur.Decode(&doc); err != nil {
			_ = cur.Close(ctx)
			return report, fmt.Errorf("decode deleted user: %w", err)
		}
		ids = append(ids, doc.ID)
	}
	if err := cur.Err(); err != nil {
		_ = cur.Close(ctx)
		return report, fmt.Errorf("iterate deleted users: %w", err)
	}
	_ = cur.Close(ctx)

	for _, oid := range ids {
		uid := oid.Hex()
		if dryRun {
			report.Users++
			continue
		}
		steps := []struct {
			collection string
			filter     bson.M
			counter    *int64
		}{
			{syncRecordsCollectionName, bson.M{"userId": uid}, &report.SyncRecords},
			{syncOperationsCollectionName, bson.M{"userId": uid}, &report.SyncOperations},
			{syncRevisionCountersCollectionName, bson.M{"_id": uid}, &report.SyncRevisionState},
			{syncUsageCollectionName, bson.M{"_id": uid}, new(int64)},
			{refreshTokensCollectionName, bson.M{"userId": uid}, &report.RefreshTokens},
			{recoverySessionsCollectionName, bson.M{"userId": uid}, &report.RecoverySessions},
			{devicesCollectionName, bson.M{"userId": uid}, &report.Devices},
			{deviceEventsCollectionName, bson.M{"userId": uid}, &report.DeviceEvents},
		}
		for _, step := range steps {
			res, err := db.Collection(step.collection).DeleteMany(ctx, step.filter)
			if err != nil {
				return report, fmt.Errorf("purge %s of user %s: %w", step.collection, uid, err)
			}
			*step.counter += res.DeletedCount
		}
		// Re-check the flag in the filter: if the owner restored the account while this run was
		// in progress, the user document must survive.
		res, err := users.DeleteOne(ctx, bson.M{"_id": oid, "isDeleted": true})
		if err != nil {
			return report, fmt.Errorf("purge user %s: %w", uid, err)
		}
		report.Users += res.DeletedCount
	}
	return report, nil
}
