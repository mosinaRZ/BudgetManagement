package sync_test

import (
	"context"
	"testing"
	"time"

	"github.com/mosinaRZ/finance-sync-backend/internal/domain/entity"
	"github.com/mosinaRZ/finance-sync-backend/internal/domain/repository"
	"github.com/mosinaRZ/finance-sync-backend/internal/usecase/mocks"
	syncUsecase "github.com/mosinaRZ/finance-sync-backend/internal/usecase/sync"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func validInput() syncUsecase.SyncInput {
	return syncUsecase.SyncInput{ProtocolVersion: 1, SchemaVersion: 1, RequestID: "req-1", DeviceID: "device-1", Changes: []syncUsecase.SyncChange{{EntityType: "TRANSACTION", EntityID: "00000000-0000-0000-0000-000000000001", Ciphertext: []byte("cipher"), Nonce: []byte("nonce"), Version: 1, UpdatedAt: time.Now().UTC(), EncryptionKeyVersion: 1}}}
}

func TestSyncUsecase_RequestIsPassedAsOneAtomicOperation(t *testing.T) {
	var got repository.SyncRequest
	repo := &mocks.MockSyncRepository{SyncFunc: func(_ context.Context, req repository.SyncRequest) (repository.SyncResult, error) {
		got = req
		return repository.SyncResult{NextCursor: 7, HasMore: true}, nil
	}}
	out, err := syncUsecase.NewSyncUsecase(repo).Execute(context.Background(), "user-1", validInput())
	require.NoError(t, err)
	assert.Equal(t, "req-1", got.RequestID)
	assert.Equal(t, "user-1", got.UserID)
	assert.Equal(t, uint64(7), out.NextCursor)
	assert.True(t, out.HasMore)
}

func TestSyncUsecase_RejectsLimitsAndInvalidRecord(t *testing.T) {
	repo := &mocks.MockSyncRepository{}
	uc := syncUsecase.NewSyncUsecase(repo)
	in := validInput()
	in.Changes = make([]syncUsecase.SyncChange, syncUsecase.MaxChangesPerRequest+1)
	_, err := uc.Execute(context.Background(), "user", in)
	require.Error(t, err)
	in = validInput()
	in.Changes[0].Ciphertext = make([]byte, syncUsecase.MaxCiphertextSize+1)
	_, err = uc.Execute(context.Background(), "user", in)
	require.Error(t, err)
}

func TestSyncUsecase_RejectsUnsupportedProtocolVersion(t *testing.T) {
	in := validInput()
	in.ProtocolVersion = 99
	_, err := syncUsecase.NewSyncUsecase(&mocks.MockSyncRepository{}).Execute(context.Background(), "user", in)
	require.Error(t, err)
}

func TestSyncUsecase_MapsConflictsAndServerChanges(t *testing.T) {
	rec := &entity.SyncRecord{UserID: "user", EntityType: entity.EntityTypeTransaction, EntityID: "id", Ciphertext: []byte("c"), Nonce: []byte("n"), Version: 2, UpdatedAt: time.Now().UTC(), ServerRevision: 12, EncryptionKeyVersion: 1}
	repo := &mocks.MockSyncRepository{SyncFunc: func(context.Context, repository.SyncRequest) (repository.SyncResult, error) {
		return repository.SyncResult{ServerChanges: []*entity.SyncRecord{rec}, Conflicts: []*entity.SyncRecord{rec}, NextCursor: 12}, nil
	}}
	out, err := syncUsecase.NewSyncUsecase(repo).Execute(context.Background(), "user", validInput())
	require.NoError(t, err)
	require.Len(t, out.ServerChanges, 1)
	require.Len(t, out.Conflicts, 1)
	assert.Equal(t, uint64(12), out.NextCursor)
}
