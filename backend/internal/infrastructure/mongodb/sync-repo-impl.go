package mongodb

import (
	"context"
	"crypto/sha256"
	"encoding/binary"
	"errors"
	"fmt"
	"time"

	"go.mongodb.org/mongo-driver/bson"
	"go.mongodb.org/mongo-driver/mongo"
	"go.mongodb.org/mongo-driver/mongo/options"
	"go.mongodb.org/mongo-driver/mongo/readpref"

	"github.com/mosinaRZ/finance-sync-backend/internal/domain/apperror"
	"github.com/mosinaRZ/finance-sync-backend/internal/domain/entity"
	"github.com/mosinaRZ/finance-sync-backend/internal/domain/repository"
	"github.com/mosinaRZ/finance-sync-backend/internal/infrastructure/mongodb/models"
)

const (
	syncRecordsCollectionName          = "sync_records"
	syncOperationsCollectionName       = "sync_operations"
	syncRevisionCountersCollectionName = "sync_revision_counters"
	// Revisions are allocated per user. This prevents unrelated users from
	// contending on one global counter and keeps cursors scoped to a user.
	maxPullBatchSize    = 100
	maxPullPayloadBytes = 4 * 1024 * 1024
)

type syncRepositoryImpl struct {
	client     *mongo.Client
	db         *mongo.Database
	collection *mongo.Collection
}

func NewSyncRepository(client *mongo.Client, db *mongo.Database) *syncRepositoryImpl {
	return &syncRepositoryImpl{client: client, db: db, collection: db.Collection(syncRecordsCollectionName)}
}

func (r *syncRepositoryImpl) Sync(ctx context.Context, req repository.SyncRequest) (repository.SyncResult, error) {
	if err := validateSyncRequest(req); err != nil {
		return repository.SyncResult{}, apperror.ErrValidation(err.Error())
	}
	ctx, cancel := context.WithTimeout(ctx, 30*time.Second)
	defer cancel()
	session, err := r.client.StartSession()
	if err != nil {
		return repository.SyncResult{}, apperror.ErrInternal("failed to start sync session", err)
	}
	defer session.EndSession(ctx)

	var result repository.SyncResult
	_, err = session.WithTransaction(ctx, func(sc mongo.SessionContext) (interface{}, error) {
		var op syncOperationModel
		err := r.db.Collection(syncOperationsCollectionName).FindOne(sc, bson.M{"userId": req.UserID, "requestId": req.RequestID}).Decode(&op)
		if err == nil {
			if op.Fingerprint != syncRequestFingerprint(req) {
				return nil, apperror.ErrConflict("sync request id was already used with different content")
			}
			result = operationToResult(&op)
			return nil, nil
		}
		if !errors.Is(err, mongo.ErrNoDocuments) {
			return nil, apperror.ErrInternal("failed to read sync operation", err)
		}

		conflicts, err := r.applyRecords(sc, req)
		if err != nil {
			return nil, err
		}
		pulled, next, more, err := r.pullPage(sc, req.UserID, req.Cursor)
		if err != nil {
			return nil, err
		}
		result = repository.SyncResult{ServerChanges: pulled, Conflicts: conflicts, NextCursor: next, HasMore: more}

		op = syncOperationModel{UserID: req.UserID, RequestID: req.RequestID, Fingerprint: syncRequestFingerprint(req), NextCursor: int64(next), HasMore: more, CreatedAt: time.Now().UTC()}
		op.ServerChanges = toModels(pulled)
		op.Conflicts = toModels(conflicts)
		if _, err = r.db.Collection(syncOperationsCollectionName).InsertOne(sc, op); err != nil {
			return nil, apperror.ErrInternal("failed to record sync operation", err)
		}
		return nil, nil
	}, options.Transaction().SetReadPreference(readpref.Primary()))
	if err != nil {
		var ae *apperror.AppError
		if errors.As(err, &ae) {
			return repository.SyncResult{}, err
		}
		return repository.SyncResult{}, apperror.ErrInternal("sync transaction failed", err)
	}
	return result, nil
}

func syncRequestFingerprint(req repository.SyncRequest) string {
	h := sha256.New()
	writeString := func(v string) {
		var b [8]byte
		binary.BigEndian.PutUint64(b[:], uint64(len(v)))
		h.Write(b[:])
		h.Write([]byte(v))
	}
	writeBytes := func(v []byte) {
		var b [8]byte
		binary.BigEndian.PutUint64(b[:], uint64(len(v)))
		h.Write(b[:])
		h.Write(v)
	}
	writeString(req.UserID)
	writeString(req.DeviceID)
	writeString(req.RequestID)
	var b [8]byte
	binary.BigEndian.PutUint64(b[:], uint64(req.Cursor))
	h.Write(b[:])
	for _, r := range req.Records {
		writeString(string(r.EntityType))
		writeString(r.EntityID)
		writeBytes(r.Ciphertext)
		writeBytes(r.Nonce)
		var x [8]byte
		binary.BigEndian.PutUint64(x[:], uint64(r.EncryptionKeyVersion))
		h.Write(x[:])
		binary.BigEndian.PutUint64(x[:], uint64(r.Version))
		h.Write(x[:])
		binary.BigEndian.PutUint64(x[:], uint64(r.UpdatedAt.UnixNano()))
		h.Write(x[:])
		if r.IsDeleted {
			h.Write([]byte{1})
		} else {
			h.Write([]byte{0})
		}
	}
	return fmt.Sprintf("%x", h.Sum(nil))
}

func validateSyncRequest(req repository.SyncRequest) error {
	if req.UserID == "" || req.RequestID == "" || req.DeviceID == "" {
		return fmt.Errorf("sync identity is required")
	}
	if len(req.Records) > 100 {
		return fmt.Errorf("too many sync changes")
	}
	seen := make(map[string]struct{}, len(req.Records))
	for _, rec := range req.Records {
		if rec == nil {
			return fmt.Errorf("sync record is required")
		}
		if err := rec.Validate(); err != nil {
			return err
		}
		key := string(rec.EntityType) + "\x00" + rec.EntityID
		if _, ok := seen[key]; ok {
			return fmt.Errorf("duplicate record identity in request")
		}
		seen[key] = struct{}{}
	}
	return nil
}

func (r *syncRepositoryImpl) applyRecords(ctx mongo.SessionContext, req repository.SyncRequest) ([]*entity.SyncRecord, error) {
	if len(req.Records) == 0 {
		return nil, nil
	}
	conflicts := make([]*entity.SyncRecord, 0)
	toWrite := make([]*entity.SyncRecord, 0, len(req.Records))
	for _, incoming := range req.Records {
		// The authenticated request user is authoritative. Never trust the
		// user ID embedded in client-provided record payloads.
		incoming.UserID = req.UserID
		existing, err := r.findExisting(ctx, req.UserID, incoming.EntityType, incoming.EntityID)
		if err != nil {
			return nil, err
		}
		if existing == nil {
			toWrite = append(toWrite, incoming)
			continue
		}
		switch {
		case incoming.Version > existing.Version:
			toWrite = append(toWrite, incoming)
		case incoming.Version < existing.Version:
			conflicts = append(conflicts, existing.ToEntity())
		case samePayload(existing, incoming):
			// Exact replay of the same logical version: idempotent no-op.
		default:
			conflicts = append(conflicts, existing.ToEntity())
		}
	}
	if len(toWrite) == 0 {
		return conflicts, nil
	}

	start, err := r.allocateRevisions(ctx, req.UserID, len(toWrite))
	if err != nil {
		return nil, err
	}
	writes := make([]mongo.WriteModel, 0, len(toWrite))
	for i, rec := range toWrite {
		rec.ServerRevision = start + uint64(i)
		model, err := models.FromEntity(rec)
		if err != nil {
			return nil, apperror.ErrInternal("invalid sync record", err)
		}
		filter := bson.M{"userId": model.UserID, "entityType": model.EntityType, "entityId": model.EntityID}
		update := bson.M{"$set": bson.M{"ciphertext": model.Ciphertext, "nonce": model.Nonce, "version": model.Version, "updatedAt": model.UpdatedAt, "isDeleted": model.IsDeleted, "deviceId": model.DeviceID, "serverRevision": model.ServerRevision, "encryptionKeyVersion": model.EncryptionKeyVersion}, "$setOnInsert": bson.M{"userId": model.UserID, "entityType": model.EntityType, "entityId": model.EntityID}}
		writes = append(writes, mongo.NewUpdateOneModel().SetFilter(filter).SetUpdate(update).SetUpsert(true))
	}
	if _, err := r.collection.BulkWrite(ctx, writes, options.BulkWrite().SetOrdered(true)); err != nil {
		return nil, apperror.ErrInternal("failed to write sync records", err)
	}
	return conflicts, nil
}

func (r *syncRepositoryImpl) findExisting(ctx mongo.SessionContext, userID string, typ entity.EntityType, id string) (*models.SyncRecordModel, error) {
	var m models.SyncRecordModel
	err := r.collection.FindOne(ctx, bson.M{"userId": userID, "entityType": string(typ), "entityId": id}).Decode(&m)
	if errors.Is(err, mongo.ErrNoDocuments) {
		return nil, nil
	}
	if err != nil {
		return nil, apperror.ErrInternal("failed to read sync record", err)
	}
	return &m, nil
}

func samePayload(existing *models.SyncRecordModel, incoming *entity.SyncRecord) bool {
	return existing.IsDeleted == incoming.IsDeleted && existing.UpdatedAt.Equal(incoming.UpdatedAt) && existing.DeviceID == incoming.DeviceID && existing.EncryptionKeyVersion == incoming.EncryptionKeyVersion && string(existing.Ciphertext) == string(incoming.Ciphertext) && string(existing.Nonce) == string(incoming.Nonce)
}

func (r *syncRepositoryImpl) allocateRevisions(ctx mongo.SessionContext, userID string, n int) (uint64, error) {
	if userID == "" || n <= 0 {
		return 0, apperror.ErrValidation("invalid sync revision allocation")
	}
	var doc struct {
		Value int64 `bson:"value"`
	}
	counter := r.db.Collection(syncRevisionCountersCollectionName)
	// Existing installations may already contain records written by the former
	// global counter. Seed the per-user counter lazily from that user's highest
	// revision; this is deliberately done inside the normal sync transaction and
	// does not require a database migration or a Room schema change.
	var maxExisting struct {
		ServerRevision int64 `bson:"serverRevision"`
	}
	if err := r.collection.FindOne(ctx, bson.M{"userId": userID}, options.FindOne().SetSort(bson.D{{Key: "serverRevision", Value: -1}}).SetProjection(bson.M{"serverRevision": 1})).Decode(&maxExisting); err != nil && !errors.Is(err, mongo.ErrNoDocuments) {
		return 0, apperror.ErrInternal("failed to inspect sync revision state", err)
	}
	filter := bson.M{"_id": userID}
	update := mongo.Pipeline{{{Key: "$set", Value: bson.M{"value": bson.M{"$add": bson.A{bson.M{"$max": bson.A{bson.M{"$ifNull": bson.A{"$value", 0}}, maxExisting.ServerRevision}}, int64(n)}}}}}}
	err := counter.FindOneAndUpdate(ctx, filter, update, options.FindOneAndUpdate().SetUpsert(true).SetReturnDocument(options.After)).Decode(&doc)
	if err != nil {
		return 0, apperror.ErrInternal("failed to allocate sync revisions", err)
	}
	if doc.Value < int64(n) {
		return 0, apperror.ErrInternal("invalid sync revision counter")
	}
	return uint64(doc.Value-int64(n)) + 1, nil
}

func (r *syncRepositoryImpl) pullPage(ctx mongo.SessionContext, userID string, cursor repository.SyncCursor) ([]*entity.SyncRecord, repository.SyncCursor, bool, error) {
	cur, err := r.collection.Find(ctx, bson.M{"userId": userID, "serverRevision": bson.M{"$gt": int64(cursor)}}, options.Find().SetSort(bson.D{{Key: "serverRevision", Value: 1}}).SetLimit(maxPullBatchSize+1))
	if err != nil {
		return nil, cursor, false, apperror.ErrInternal("failed to query sync changes", err)
	}
	defer cur.Close(ctx)
	records := make([]*entity.SyncRecord, 0, maxPullBatchSize)
	payloadBytes := 0
	hasMore := false
	for cur.Next(ctx) {
		var m models.SyncRecordModel
		if err := cur.Decode(&m); err != nil {
			return nil, cursor, false, apperror.ErrInternal("failed to decode sync change", err)
		}
		recordBytes := len(m.Ciphertext) + len(m.Nonce)
		if len(records) >= maxPullBatchSize || (len(records) > 0 && payloadBytes+recordBytes > maxPullPayloadBytes) {
			hasMore = true
			break
		}
		records = append(records, m.ToEntity())
		payloadBytes += recordBytes
	}
	if err := cur.Err(); err != nil {
		return nil, cursor, false, apperror.ErrInternal("failed to iterate sync changes", err)
	}
	next := cursor
	if len(records) > 0 {
		next = repository.SyncCursor(records[len(records)-1].ServerRevision)
	}
	return records, next, hasMore, nil
}

func toModels(in []*entity.SyncRecord) []models.SyncRecordModel {
	out := make([]models.SyncRecordModel, 0, len(in))
	for _, e := range in {
		m, _ := models.FromEntity(e)
		out = append(out, *m)
	}
	return out
}
func operationToResult(op *syncOperationModel) repository.SyncResult {
	server := make([]*entity.SyncRecord, 0, len(op.ServerChanges))
	for i := range op.ServerChanges {
		server = append(server, op.ServerChanges[i].ToEntity())
	}
	conf := make([]*entity.SyncRecord, 0, len(op.Conflicts))
	for i := range op.Conflicts {
		conf = append(conf, op.Conflicts[i].ToEntity())
	}
	return repository.SyncResult{ServerChanges: server, Conflicts: conf, NextCursor: repository.SyncCursor(op.NextCursor), HasMore: op.HasMore}
}

var _ repository.SyncRepository = (*syncRepositoryImpl)(nil)
