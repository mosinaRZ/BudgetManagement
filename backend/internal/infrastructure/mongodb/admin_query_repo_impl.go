package mongodb

import (
	"context"
	"fmt"
	"regexp"
	"strings"
	"time"

	"github.com/mosinaRZ/finance-sync-backend/internal/domain/apperror"
	"github.com/mosinaRZ/finance-sync-backend/internal/domain/entity"
	"github.com/mosinaRZ/finance-sync-backend/internal/domain/repository"
	"github.com/mosinaRZ/finance-sync-backend/internal/infrastructure/mongodb/models"
	"go.mongodb.org/mongo-driver/bson"
	"go.mongodb.org/mongo-driver/bson/primitive"
	"go.mongodb.org/mongo-driver/mongo"
	"go.mongodb.org/mongo-driver/mongo/options"
)

// adminUserProjection keeps credential material out of memory entirely: the admin console only
// needs descriptive fields, so the secrets are never even read from the database.
var adminUserProjection = bson.M{
	"passwordHash": 0, "authSalt": 0, "kdfSalt": 0, "passwordKeyEnvelope": 0, "passwordKeyNonce": 0,
	"recoveryKeyHash": 0, "recoveryKeyEnvelope": 0, "recoveryKeyNonce": 0, "phoneHash": 0,
}

type adminQueryRepo struct{ db *mongo.Database }

func NewAdminQueryRepository(db *mongo.Database) *adminQueryRepo { return &adminQueryRepo{db: db} }

var nonDigits = regexp.MustCompile(`\D`)

func toObjectIDs(ids []string) []primitive.ObjectID {
	out := make([]primitive.ObjectID, 0, len(ids))
	for _, id := range ids {
		if oid, err := primitive.ObjectIDFromHex(id); err == nil {
			out = append(out, oid)
		}
	}
	return out
}

func (r *adminQueryRepo) onlineUserIDs(ctx context.Context, window time.Duration) ([]string, error) {
	cutoff := time.Now().UTC().Add(-window)
	vals, err := r.db.Collection(devicesCollectionName).Distinct(ctx, "userId", bson.M{"lastSeenAt": bson.M{"$gte": cutoff}})
	if err != nil {
		return nil, apperror.ErrInternal("failed to read presence", err)
	}
	ids := make([]string, 0, len(vals))
	for _, v := range vals {
		if s, ok := v.(string); ok && s != "" {
			ids = append(ids, s)
		}
	}
	return ids, nil
}

func (r *adminQueryRepo) userQuery(ctx context.Context, f repository.AdminUserFilter) (bson.M, error) {
	q := bson.M{}
	and := []bson.M{}
	switch f.Role {
	case string(entity.RoleUser):
		// documents written before roles existed have no role field: they are plain users
		and = append(and, bson.M{"$or": []bson.M{{"role": string(entity.RoleUser)}, {"role": bson.M{"$exists": false}}}})
	case string(entity.RolePro), string(entity.RolePremium), string(entity.RoleAdmin):
		q["role"] = f.Role
	}
	switch f.Status {
	case "active":
		and = append(and, bson.M{"isDeleted": bson.M{"$ne": true}})
	case "deleted":
		q["isDeleted"] = true
	case "locked":
		and = append(and, bson.M{"isDeleted": bson.M{"$ne": true}}, bson.M{"lockedUntil": bson.M{"$gt": time.Now().UTC()}})
	}
	if s := strings.TrimSpace(f.Search); s != "" {
		if len(s) > 48 {
			s = s[:48]
		}
		or := []bson.M{}
		if oid, err := primitive.ObjectIDFromHex(s); err == nil {
			or = append(or, bson.M{"_id": oid})
		}
		rx := primitive.Regex{Pattern: regexp.QuoteMeta(s), Options: "i"}
		or = append(or, bson.M{"phoneNumber": rx}, bson.M{"firstName": rx}, bson.M{"lastName": rx})
		if digits := nonDigits.ReplaceAllString(s, ""); len(digits) >= 3 {
			digits = strings.TrimLeft(digits, "0")
			if len(digits) >= 3 {
				or = append(or, bson.M{"phoneNumber": primitive.Regex{Pattern: regexp.QuoteMeta(digits)}})
			}
		}
		if parts := strings.Fields(s); len(parts) >= 2 {
			or = append(or, bson.M{"$and": []bson.M{
				{"firstName": primitive.Regex{Pattern: regexp.QuoteMeta(parts[0]), Options: "i"}},
				{"lastName": primitive.Regex{Pattern: regexp.QuoteMeta(strings.Join(parts[1:], " ")), Options: "i"}},
			}})
		}
		and = append(and, bson.M{"$or": or})
	}
	if f.Presence == "online" || f.Presence == "offline" {
		ids, err := r.onlineUserIDs(ctx, f.OnlineWindow)
		if err != nil {
			return nil, err
		}
		oids := toObjectIDs(ids)
		if f.Presence == "online" {
			and = append(and, bson.M{"_id": bson.M{"$in": oids}})
		} else {
			and = append(and, bson.M{"_id": bson.M{"$nin": oids}})
		}
	}
	if len(and) > 0 {
		q["$and"] = and
	}
	return q, nil
}

func (r *adminQueryRepo) ListUsers(ctx context.Context, f repository.AdminUserFilter) ([]repository.AdminUserRow, int64, error) {
	q, err := r.userQuery(ctx, f)
	if err != nil {
		return nil, 0, err
	}
	users := r.db.Collection(usersCollectionName)
	total, err := users.CountDocuments(ctx, q)
	if err != nil {
		return nil, 0, apperror.ErrInternal("failed to count users", err)
	}
	limit := int64(f.Limit)
	if limit <= 0 || limit > 100 {
		limit = 30
	}
	var sort bson.D
	switch f.Sort {
	case "oldest":
		sort = bson.D{{Key: "createdAt", Value: 1}, {Key: "_id", Value: 1}}
	case "name":
		sort = bson.D{{Key: "firstName", Value: 1}, {Key: "lastName", Value: 1}, {Key: "_id", Value: 1}}
	default:
		sort = bson.D{{Key: "createdAt", Value: -1}, {Key: "_id", Value: -1}}
	}
	opts := options.Find().SetSort(sort).SetLimit(limit).SetProjection(adminUserProjection)
	if f.Offset > 0 {
		opts.SetSkip(int64(f.Offset))
	}
	cur, err := users.Find(ctx, q, opts)
	if err != nil {
		return nil, 0, apperror.ErrInternal("failed to list users", err)
	}
	defer cur.Close(ctx)
	rows := make([]repository.AdminUserRow, 0, limit)
	ids := make([]string, 0, limit)
	for cur.Next(ctx) {
		var m models.UserModel
		if err := cur.Decode(&m); err != nil {
			return nil, 0, apperror.ErrInternal("failed to decode user", err)
		}
		u := m.ToEntity()
		rows = append(rows, repository.AdminUserRow{User: u})
		ids = append(ids, u.ID)
	}
	if err := cur.Err(); err != nil {
		return nil, 0, apperror.ErrInternal("failed to read users", err)
	}
	if err := r.enrich(ctx, rows, ids, f.OnlineWindow); err != nil {
		return nil, 0, err
	}
	return rows, total, nil
}

func (r *adminQueryRepo) GetUser(ctx context.Context, id string, onlineWindow time.Duration) (*repository.AdminUserRow, error) {
	oid, err := primitive.ObjectIDFromHex(id)
	if err != nil {
		return nil, apperror.ErrNotFound("user not found")
	}
	var m models.UserModel
	err = r.db.Collection(usersCollectionName).FindOne(ctx, bson.M{"_id": oid}, options.FindOne().SetProjection(adminUserProjection)).Decode(&m)
	if err == mongo.ErrNoDocuments {
		return nil, apperror.ErrNotFound("user not found")
	}
	if err != nil {
		return nil, apperror.ErrInternal("failed to load user", err)
	}
	rows := []repository.AdminUserRow{{User: m.ToEntity()}}
	if err := r.enrich(ctx, rows, []string{id}, onlineWindow); err != nil {
		return nil, err
	}
	return &rows[0], nil
}

type adminDeviceDoc struct {
	UserID     string    `bson:"userId"`
	DeviceID   string    `bson:"deviceId"`
	Name       string    `bson:"name"`
	Model      string    `bson:"model"`
	Platform   string    `bson:"platform"`
	OSVersion  string    `bson:"osVersion"`
	AppVersion string    `bson:"appVersion"`
	LastIP     string    `bson:"lastIp"`
	LastSeenAt time.Time `bson:"lastSeenAt"`
	CreatedAt  time.Time `bson:"createdAt"`
}

func (r *adminQueryRepo) enrich(ctx context.Context, rows []repository.AdminUserRow, ids []string, window time.Duration) error {
	if len(rows) == 0 {
		return nil
	}
	if window <= 0 {
		window = 3 * time.Minute
	}
	index := make(map[string]int, len(rows))
	for i := range rows {
		index[rows[i].User.ID] = i
	}
	cur, err := r.db.Collection(devicesCollectionName).Find(ctx, bson.M{"userId": bson.M{"$in": ids}},
		options.Find().SetSort(bson.D{{Key: "createdAt", Value: 1}}))
	if err != nil {
		return apperror.ErrInternal("failed to load devices", err)
	}
	defer cur.Close(ctx)
	cutoff := time.Now().UTC().Add(-window)
	for cur.Next(ctx) {
		var d adminDeviceDoc
		if err := cur.Decode(&d); err != nil {
			return apperror.ErrInternal("failed to decode device", err)
		}
		i, ok := index[d.UserID]
		if !ok {
			continue
		}
		rows[i].Devices = append(rows[i].Devices, &entity.Device{
			ID: d.DeviceID, UserID: d.UserID, Name: d.Name, Model: d.Model, Platform: d.Platform,
			OSVersion: d.OSVersion, AppVersion: d.AppVersion, LastIP: d.LastIP, LastSeenAt: d.LastSeenAt, CreatedAt: d.CreatedAt,
		})
		if d.LastSeenAt.After(rows[i].LastSeenAt) {
			rows[i].LastSeenAt = d.LastSeenAt
		}
		if d.LastSeenAt.After(cutoff) && !rows[i].User.IsDeleted {
			rows[i].Online = true
		}
	}
	if err := cur.Err(); err != nil {
		return apperror.ErrInternal("failed to read devices", err)
	}
	uc, err := r.db.Collection(syncUsageCollectionName).Find(ctx, bson.M{"_id": bson.M{"$in": ids}})
	if err != nil {
		return apperror.ErrInternal("failed to load sync usage", err)
	}
	defer uc.Close(ctx)
	for uc.Next(ctx) {
		var d struct {
			ID      string `bson:"_id"`
			Records int64  `bson:"records"`
			Bytes   int64  `bson:"bytes"`
		}
		if err := uc.Decode(&d); err != nil {
			return apperror.ErrInternal("failed to decode sync usage", err)
		}
		if i, ok := index[d.ID]; ok {
			rows[i].SyncRecords, rows[i].SyncBytes = d.Records, d.Bytes
		}
	}
	if err := uc.Err(); err != nil {
		return apperror.ErrInternal("failed to read sync usage", err)
	}
	return nil
}

func (r *adminQueryRepo) CountActiveAdmins(ctx context.Context) (int64, error) {
	n, err := r.db.Collection(usersCollectionName).CountDocuments(ctx, bson.M{"role": string(entity.RoleAdmin), "isDeleted": bson.M{"$ne": true}})
	if err != nil {
		return 0, apperror.ErrInternal("failed to count admins", err)
	}
	return n, nil
}

func (r *adminQueryRepo) Labels(ctx context.Context, ids []string) (map[string]string, error) {
	out := map[string]string{}
	oids := toObjectIDs(ids)
	if len(oids) == 0 {
		return out, nil
	}
	cur, err := r.db.Collection(usersCollectionName).Find(ctx, bson.M{"_id": bson.M{"$in": oids}},
		options.Find().SetProjection(bson.M{"firstName": 1, "lastName": 1, "phoneNumber": 1}))
	if err != nil {
		return nil, apperror.ErrInternal("failed to load user labels", err)
	}
	defer cur.Close(ctx)
	for cur.Next(ctx) {
		var d struct {
			ID    primitive.ObjectID `bson:"_id"`
			First string             `bson:"firstName"`
			Last  string             `bson:"lastName"`
			Phone string             `bson:"phoneNumber"`
		}
		if err := cur.Decode(&d); err != nil {
			return nil, apperror.ErrInternal("failed to decode user label", err)
		}
		label := strings.TrimSpace(d.First + " " + d.Last)
		if label == "" {
			label = d.Phone
		}
		out[d.ID.Hex()] = label
	}
	return out, cur.Err()
}

func tzString(min int) string {
	if min < -720 {
		min = -720
	}
	if min > 840 {
		min = 840
	}
	sign := "+"
	if min < 0 {
		sign, min = "-", -min
	}
	return fmt.Sprintf("%s%02d:%02d", sign, min/60, min%60)
}

func (r *adminQueryRepo) buckets(ctx context.Context, coll string, pipeline mongo.Pipeline) ([]repository.Bucket, error) {
	cur, err := r.db.Collection(coll).Aggregate(ctx, pipeline)
	if err != nil {
		return nil, apperror.ErrInternal("failed to aggregate", err)
	}
	defer cur.Close(ctx)
	out := []repository.Bucket{}
	for cur.Next(ctx) {
		var d struct {
			Key   string `bson:"_id"`
			Count int64  `bson:"count"`
		}
		if err := cur.Decode(&d); err != nil {
			return nil, apperror.ErrInternal("failed to decode aggregate", err)
		}
		out = append(out, repository.Bucket{Key: d.Key, Count: d.Count})
	}
	return out, cur.Err()
}

func (r *adminQueryRepo) days(ctx context.Context, coll string, pipeline mongo.Pipeline) ([]repository.DayPoint, error) {
	cur, err := r.db.Collection(coll).Aggregate(ctx, pipeline)
	if err != nil {
		return nil, apperror.ErrInternal("failed to aggregate", err)
	}
	defer cur.Close(ctx)
	out := []repository.DayPoint{}
	for cur.Next(ctx) {
		var d struct {
			Day     string `bson:"_id"`
			Records int64  `bson:"records"`
			Bytes   int64  `bson:"bytes"`
		}
		if err := cur.Decode(&d); err != nil {
			return nil, apperror.ErrInternal("failed to decode aggregate", err)
		}
		out = append(out, repository.DayPoint{Day: d.Day, Records: d.Records, Bytes: d.Bytes})
	}
	return out, cur.Err()
}

func stage(key string, value interface{}) bson.D { return bson.D{{Key: key, Value: value}} }

func (r *adminQueryRepo) Overview(ctx context.Context, from, to time.Time, tzOffsetMin int, onlineWindow time.Duration, scanCap int64) (repository.AdminOverview, error) {
	var o repository.AdminOverview
	users := r.db.Collection(usersCollectionName)
	now := time.Now().UTC()
	var err error
	if o.TotalUsers, err = users.CountDocuments(ctx, bson.M{}); err != nil {
		return o, apperror.ErrInternal("failed to count users", err)
	}
	if o.DeletedUsers, err = users.CountDocuments(ctx, bson.M{"isDeleted": true}); err != nil {
		return o, apperror.ErrInternal("failed to count users", err)
	}
	o.ActiveUsers = o.TotalUsers - o.DeletedUsers
	if o.LockedUsers, err = users.CountDocuments(ctx, bson.M{"isDeleted": bson.M{"$ne": true}, "lockedUntil": bson.M{"$gt": now}}); err != nil {
		return o, apperror.ErrInternal("failed to count users", err)
	}
	if o.NewInPeriod, err = users.CountDocuments(ctx, bson.M{"createdAt": bson.M{"$gte": from, "$lt": to}}); err != nil {
		return o, apperror.ErrInternal("failed to count users", err)
	}
	if ids, err := r.onlineUserIDs(ctx, onlineWindow); err != nil {
		return o, err
	} else {
		o.OnlineNow = int64(len(ids))
	}
	tz := tzString(tzOffsetMin)
	dayKey := func(field string) bson.M {
		return bson.M{"$dateToString": bson.M{"format": "%Y-%m-%d", "date": "$" + field, "timezone": tz}}
	}
	if o.SignupsByDay, err = r.days(ctx, usersCollectionName, mongo.Pipeline{
		stage("$match", bson.M{"createdAt": bson.M{"$gte": from, "$lt": to}}),
		stage("$group", bson.M{"_id": dayKey("createdAt"), "records": bson.M{"$sum": 1}}),
		stage("$sort", bson.M{"_id": 1}),
	}); err != nil {
		return o, err
	}
	activeOnly := stage("$match", bson.M{"isDeleted": bson.M{"$ne": true}})
	if o.Roles, err = r.buckets(ctx, usersCollectionName, mongo.Pipeline{
		activeOnly,
		stage("$group", bson.M{"_id": bson.M{"$ifNull": bson.A{"$role", string(entity.RoleUser)}}, "count": bson.M{"$sum": 1}}),
		stage("$sort", bson.M{"count": -1}),
	}); err != nil {
		return o, err
	}
	if o.Platforms, err = r.buckets(ctx, devicesCollectionName, mongo.Pipeline{
		stage("$group", bson.M{"_id": bson.M{"$cond": bson.A{bson.M{"$in": bson.A{"$platform", bson.A{nil, ""}}}, "unknown", "$platform"}}, "count": bson.M{"$sum": 1}}),
		stage("$sort", bson.M{"count": -1}),
	}); err != nil {
		return o, err
	}
	if o.AppVersions, err = r.buckets(ctx, devicesCollectionName, mongo.Pipeline{
		stage("$group", bson.M{"_id": bson.M{"$cond": bson.A{bson.M{"$in": bson.A{"$appVersion", bson.A{nil, ""}}}, "unknown", "$appVersion"}}, "count": bson.M{"$sum": 1}}),
		stage("$sort", bson.M{"count": -1}),
		stage("$limit", 8),
	}); err != nil {
		return o, err
	}

	// Sync totals come from the per-account counters that are kept up to date on every push.
	cur, err := r.db.Collection(syncUsageCollectionName).Aggregate(ctx, mongo.Pipeline{
		stage("$group", bson.M{"_id": nil, "records": bson.M{"$sum": "$records"}, "bytes": bson.M{"$sum": "$bytes"}}),
	})
	if err != nil {
		return o, apperror.ErrInternal("failed to aggregate sync usage", err)
	}
	if cur.Next(ctx) {
		var d struct {
			Records int64 `bson:"records"`
			Bytes   int64 `bson:"bytes"`
		}
		if err := cur.Decode(&d); err == nil {
			o.SyncRecords, o.SyncBytes = d.Records, d.Bytes
		}
	}
	cur.Close(ctx)

	if scanCap <= 0 {
		scanCap = 200000
	}
	if o.SyncByDay, err = r.days(ctx, syncRecordsCollectionName, mongo.Pipeline{
		stage("$match", bson.M{"updatedAt": bson.M{"$gte": from, "$lt": to}}),
		stage("$limit", scanCap),
		stage("$group", bson.M{"_id": dayKey("updatedAt"), "records": bson.M{"$sum": 1}, "bytes": bson.M{"$sum": bson.M{"$binarySize": bson.M{"$ifNull": bson.A{"$ciphertext", ""}}}}}),
		stage("$sort", bson.M{"_id": 1}),
	}); err != nil {
		return o, err
	}
	if o.SyncByEntity, err = r.buckets(ctx, syncRecordsCollectionName, mongo.Pipeline{
		stage("$match", bson.M{"isDeleted": bson.M{"$ne": true}}),
		stage("$limit", scanCap),
		stage("$group", bson.M{"_id": "$entityType", "count": bson.M{"$sum": 1}}),
		stage("$sort", bson.M{"count": -1}),
	}); err != nil {
		return o, err
	}
	top, err := r.db.Collection(syncUsageCollectionName).Find(ctx, bson.M{}, options.Find().SetSort(bson.D{{Key: "bytes", Value: -1}}).SetLimit(5))
	if err != nil {
		return o, apperror.ErrInternal("failed to load top sync users", err)
	}
	defer top.Close(ctx)
	for top.Next(ctx) {
		var d struct {
			ID      string `bson:"_id"`
			Records int64  `bson:"records"`
			Bytes   int64  `bson:"bytes"`
		}
		if err := top.Decode(&d); err != nil {
			return o, apperror.ErrInternal("failed to decode top sync users", err)
		}
		o.TopSyncUsers = append(o.TopSyncUsers, repository.SyncUserUsage{UserID: d.ID, Records: d.Records, Bytes: d.Bytes})
	}
	return o, top.Err()
}
