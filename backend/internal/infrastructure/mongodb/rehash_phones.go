package mongodb

import (
	"context"
	"fmt"

	"go.mongodb.org/mongo-driver/bson"
	"go.mongodb.org/mongo-driver/bson/primitive"
	"go.mongodb.org/mongo-driver/mongo"
	"go.mongodb.org/mongo-driver/mongo/options"
)

// RehashPhonesReport counts what RehashPhones did (or, in a dry run, would do).
type RehashPhonesReport struct {
	Scanned   int64 // users that have a stored phone number
	Updated   int64 // users whose phoneHash changed
	Unchanged int64 // users whose phoneHash already matched
	NoPhone   int64 // users without a stored phone number; they cannot be rehashed
}

// RehashPhones recomputes users.phoneHash from the stored phone number with hash, so that a
// rotated phone-hash key does not detach existing accounts from their phone number.
//
// It only touches phoneHash. It is idempotent and safe to interrupt: users that were already
// converted are skipped on the next run. Run it right after the API is restarted with the new
// PHONE_HASH_KEY; until it finishes, users that were not converted yet cannot sign in by phone.
func RehashPhones(ctx context.Context, db *mongo.Database, hash func(phone string) string, dryRun bool) (RehashPhonesReport, error) {
	var report RehashPhonesReport
	users := db.Collection(usersCollectionName)
	cur, err := users.Find(ctx, bson.M{}, options.Find().SetProjection(bson.M{"_id": 1, "phoneNumber": 1, "phoneHash": 1}))
	if err != nil {
		return report, fmt.Errorf("find users: %w", err)
	}
	defer func() { _ = cur.Close(ctx) }()

	type row struct {
		ID          primitive.ObjectID `bson:"_id"`
		PhoneNumber string             `bson:"phoneNumber"`
		PhoneHash   string             `bson:"phoneHash"`
	}
	var todo []row
	for cur.Next(ctx) {
		var r row
		if err := cur.Decode(&r); err != nil {
			return report, fmt.Errorf("decode user: %w", err)
		}
		if r.PhoneNumber == "" {
			report.NoPhone++
			continue
		}
		report.Scanned++
		if hash(r.PhoneNumber) == r.PhoneHash {
			report.Unchanged++
			continue
		}
		todo = append(todo, r)
	}
	if err := cur.Err(); err != nil {
		return report, fmt.Errorf("iterate users: %w", err)
	}
	for _, r := range todo {
		if !dryRun {
			res, err := users.UpdateOne(ctx, bson.M{"_id": r.ID, "phoneHash": r.PhoneHash}, bson.M{"$set": bson.M{"phoneHash": hash(r.PhoneNumber)}})
			if err != nil {
				return report, fmt.Errorf("update user %s: %w", r.ID.Hex(), err)
			}
			if res.ModifiedCount == 0 {
				// Changed concurrently; the next run will look at it again.
				continue
			}
		}
		report.Updated++
	}
	return report, nil
}
