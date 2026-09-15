package mongodb

import (
	"context"
	"fmt"
	"os"
	"sync"
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
	"go.mongodb.org/mongo-driver/bson"

	"github.com/mosinaRZ/finance-sync-backend/internal/domain/entity"
	"github.com/mosinaRZ/finance-sync-backend/internal/domain/repository"
)

func integrationRepo(t *testing.T) (*syncRepositoryImpl, func()) {
	t.Helper()
	uri := os.Getenv("MONGO_TEST_URI")
	if uri == "" {
		t.Skip("MONGO_TEST_URI is not set; Mongo integration tests require a replica set")
	}
	ctx, cancel := context.WithTimeout(context.Background(), 15*time.Second)
	client, err := NewClient(ctx, uri)
	cancel()
	require.NoError(t, err)
	dbName := os.Getenv("MONGO_TEST_DB")
	if dbName == "" {
		dbName = "cidna_sync_test"
	}
	db := client.Database(dbName)
	require.NoError(t, EnsureIndexes(context.Background(), db))
	require.NoError(t, db.Collection(syncRecordsCollectionName).Drop(context.Background()))
	require.NoError(t, db.Collection(syncOperationsCollectionName).Drop(context.Background()))
	require.NoError(t, db.Collection(syncRevisionCountersCollectionName).Drop(context.Background()))
	require.NoError(t, EnsureIndexes(context.Background(), db))
	return NewSyncRepository(client, db), func() { _ = db.Drop(context.Background()); _ = Disconnect(client) }
}

func syncReq(id, device string, cursor uint64, records ...*entity.SyncRecord) repository.SyncRequest {
	return repository.SyncRequest{RequestID: id, UserID: "user-test", DeviceID: device, Cursor: repository.SyncCursor(cursor), Records: records}
}
func rec(id string, version int, ts time.Time, deleted bool, device string) *entity.SyncRecord {
	return &entity.SyncRecord{UserID: "user-test", EntityType: entity.EntityTypeTransaction, EntityID: id, Ciphertext: []byte("cipher-" + id), Nonce: []byte("nonce"), Version: version, UpdatedAt: ts, IsDeleted: deleted, DeviceID: device}
}

func TestSyncRepository_TwoSameTimestampsUseVersion(t *testing.T) {
	r, done := integrationRepo(t)
	defer done()
	ts := time.Unix(100, 0).UTC()
	ctx := context.Background()
	_, err := r.Sync(ctx, syncReq("a", "A", 0, rec("x", 1, ts, false, "A")))
	require.NoError(t, err)
	out, err := r.Sync(ctx, syncReq("b", "B", 0, rec("x", 1, ts, false, "B")))
	require.NoError(t, err)
	require.Len(t, out.Conflicts, 1)
	assert.Equal(t, "A", out.Conflicts[0].DeviceID)
	out, err = r.Sync(ctx, syncReq("c", "B", 0, rec("x", 2, ts, false, "B")))
	require.NoError(t, err)
	assert.Empty(t, out.Conflicts)
}

func TestSyncRepository_RetryAfterCommitIsIdempotent(t *testing.T) {
	r, done := integrationRepo(t)
	defer done()
	ctx := context.Background()
	request := syncReq("retry-1", "A", 0, rec("x", 1, time.Now().UTC(), false, "A"))
	first, err := r.Sync(ctx, request)
	require.NoError(t, err)
	second, err := r.Sync(ctx, request)
	require.NoError(t, err)
	assert.Equal(t, first.NextCursor, second.NextCursor)
	assert.Equal(t, first.HasMore, second.HasMore)
	require.Len(t, second.ServerChanges, 1)
	assert.Equal(t, first.ServerChanges[0].ServerRevision, second.ServerChanges[0].ServerRevision)
}

func TestSyncRepository_ConcurrentDuplicateRequestHasSingleEffect(t *testing.T) {
	r, done := integrationRepo(t)
	defer done()
	ctx := context.Background()
	request := syncReq("same-request", "A", 0, rec("x", 1, time.Now().UTC(), false, "A"))
	const n = 8
	results := make(chan repository.SyncResult, n)
	errs := make(chan error, n)
	var wg sync.WaitGroup
	for i := 0; i < n; i++ {
		wg.Add(1)
		go func() { defer wg.Done(); out, err := r.Sync(ctx, request); results <- out; errs <- err }()
	}
	wg.Wait()
	close(results)
	close(errs)
	for err := range errs {
		require.NoError(t, err)
	}
	var revision uint64
	for out := range results {
		require.Len(t, out.ServerChanges, 1)
		if revision == 0 {
			revision = out.ServerChanges[0].ServerRevision
		} else {
			assert.Equal(t, revision, out.ServerChanges[0].ServerRevision)
		}
	}
	count, err := r.collection.CountDocuments(ctx, bson.M{"userId": "user-test"})
	require.NoError(t, err)
	assert.Equal(t, int64(1), count)
}

func TestSyncRepository_TwoDevicesSameRecordDeterministic(t *testing.T) {
	r, done := integrationRepo(t)
	defer done()
	ts := time.Now().UTC()
	ctx := context.Background()
	_, err := r.Sync(ctx, syncReq("d1", "A", 0, rec("x", 1, ts, false, "A")))
	require.NoError(t, err)
	out, err := r.Sync(ctx, syncReq("d2", "B", 0, rec("x", 1, ts, false, "B")))
	require.NoError(t, err)
	require.Len(t, out.Conflicts, 1)
	assert.Equal(t, "A", out.Conflicts[0].DeviceID)
	out, err = r.Sync(ctx, syncReq("d3", "B", 0, rec("x", 2, ts.Add(time.Second), false, "B")))
	require.NoError(t, err)
	assert.Empty(t, out.Conflicts)
}

func TestSyncRepository_DeleteBeatsOldUpdate(t *testing.T) {
	r, done := integrationRepo(t)
	defer done()
	ctx := context.Background()
	ts := time.Now().UTC()
	_, err := r.Sync(ctx, syncReq("a", "A", 0, rec("x", 3, ts, true, "A")))
	require.NoError(t, err)
	out, err := r.Sync(ctx, syncReq("b", "B", 0, rec("x", 2, ts.Add(time.Second), false, "B")))
	require.NoError(t, err)
	require.Len(t, out.Conflicts, 1)
	assert.True(t, out.Conflicts[0].IsDeleted)
}

func TestSyncRepository_PaginationAndCursorNoSkip(t *testing.T) {
	r, done := integrationRepo(t)
	defer done()
	ctx := context.Background()
	all := make([]*entity.SyncRecord, 0, 205)
	for i := 0; i < 205; i++ {
		id := fmt.Sprintf("%08d-0000-0000-0000-000000000000", i)
		all = append(all, rec(id, 1, time.Unix(2000, int64(i)).UTC(), false, "seed"))
	}
	for batch := 0; batch < 3; batch++ {
		start, end := batch*100, (batch+1)*100
		if end > len(all) {
			end = len(all)
		}
		_, err := r.Sync(ctx, syncReq(fmt.Sprintf("push-%d", batch), "seed", 0, all[start:end]...))
		require.NoError(t, err)
	}
	cursor := uint64(0)
	seen := map[uint64]bool{}
	total := 0
	for {
		out, err := r.Sync(ctx, syncReq(fmt.Sprintf("pull-%d", cursor), "reader", cursor))
		require.NoError(t, err)
		for _, x := range out.ServerChanges {
			require.False(t, seen[x.ServerRevision])
			seen[x.ServerRevision] = true
			total++
		}
		if !out.HasMore {
			cursor = uint64(out.NextCursor)
			break
		}
		require.Greater(t, uint64(out.NextCursor), cursor)
		cursor = uint64(out.NextCursor)
	}
	assert.Equal(t, 205, total)
	assert.Len(t, seen, 205)
}

func TestSyncRepository_SameCursorCanBeRetriedAfterClientCrash(t *testing.T) {
	r, done := integrationRepo(t)
	defer done()
	ctx := context.Background()
	_, err := r.Sync(ctx, syncReq("seed", "A", 0, rec("x", 1, time.Now().UTC(), false, "A")))
	require.NoError(t, err)
	first, err := r.Sync(ctx, syncReq("pull-1", "B", 0))
	require.NoError(t, err)
	second, err := r.Sync(ctx, syncReq("pull-2", "B", 0))
	require.NoError(t, err)
	require.Len(t, first.ServerChanges, 1)
	require.Len(t, second.ServerChanges, 1)
	assert.Equal(t, first.ServerChanges[0].ServerRevision, second.ServerChanges[0].ServerRevision)
}

func TestSyncRepository_CancelledTransactionLeavesNoWrite(t *testing.T) {
	r, done := integrationRepo(t)
	defer done()
	ctx, cancel := context.WithCancel(context.Background())
	cancel()
	_, err := r.Sync(ctx, syncReq("cancelled", "A", 0, rec("x", 1, time.Now().UTC(), false, "A")))
	require.Error(t, err)
	count, err := r.collection.CountDocuments(context.Background(), bson.M{"userId": "user-test"})
	require.NoError(t, err)
	assert.Zero(t, count)
}

func TestSyncRepository_StaleVersionNeverResurrectsTombstone(t *testing.T) {
	r, done := integrationRepo(t)
	defer done()
	ctx := context.Background()
	ts := time.Now().UTC()
	_, err := r.Sync(ctx, syncReq("del", "A", 0, rec("x", 5, ts, true, "A")))
	require.NoError(t, err)
	out, err := r.Sync(ctx, syncReq("old", "B", 0, rec("x", 4, ts.Add(time.Hour), false, "B")))
	require.NoError(t, err)
	require.Len(t, out.Conflicts, 1)
	assert.True(t, out.Conflicts[0].IsDeleted)
}

func TestSyncRepository_RequestIDDifferentContentIsConflict(t *testing.T) {
	r, done := integrationRepo(t)
	defer done()
	ctx := context.Background()
	request := syncReq("same-id", "A", 0, rec("x", 1, time.Now().UTC(), false, "A"))
	_, err := r.Sync(ctx, request)
	require.NoError(t, err)
	request.DeviceID = "B"
	_, err = r.Sync(ctx, request)
	require.Error(t, err)
}

func TestSyncRepository_ConcurrentWritesSameRecordRemainConsistent(t *testing.T) {
	r, done := integrationRepo(t)
	defer done()
	ctx := context.Background()
	start := make(chan struct{})
	results := make(chan error, 2)
	for i, device := range []string{"A", "B"} {
		go func(i int, device string) {
			<-start
			_, err := r.Sync(ctx, syncReq(fmt.Sprintf("concurrent-%d", i), device, 0, rec("x", 1, time.Unix(500, 0).UTC(), false, device)))
			results <- err
		}(i, device)
	}
	close(start)
	err1, err2 := <-results, <-results
	require.True(t, err1 == nil || err2 == nil, "at least one concurrent write must succeed: %v / %v", err1, err2)
	count, err := r.collection.CountDocuments(ctx, bson.M{"userId": "user-test", "entityType": string(entity.EntityTypeTransaction), "entityId": "x"})
	require.NoError(t, err)
	assert.Equal(t, int64(1), count)
}
