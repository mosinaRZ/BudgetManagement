package mongodb

import (
	"context"
	"os"
	"testing"
	"time"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
	"go.mongodb.org/mongo-driver/mongo"

	"github.com/mosinaRZ/finance-sync-backend/internal/domain/entity"
)

func authIntegrationDB(t *testing.T) (*mongo.Database, func()) {
	t.Helper()
	uri := os.Getenv("MONGO_TEST_URI")
	if uri == "" {
		t.Skip("MONGO_TEST_URI is not set; Mongo integration tests require a replica set")
	}
	ctx, cancel := context.WithTimeout(context.Background(), 15*time.Second)
	defer cancel()
	client, err := NewClient(ctx, uri)
	require.NoError(t, err)
	dbName := os.Getenv("MONGO_TEST_DB")
	if dbName == "" {
		dbName = "cidna_auth_test"
	}
	db := client.Database(dbName)
	require.NoError(t, EnsureIndexes(context.Background(), db))
	require.NoError(t, db.Collection(usersCollectionName).Drop(context.Background()))
	require.NoError(t, db.Collection(refreshTokensCollectionName).Drop(context.Background()))
	require.NoError(t, EnsureIndexes(context.Background(), db))
	return db, func() { _ = db.Drop(context.Background()); _ = Disconnect(client) }
}

func TestUserRepository_UniquePhoneHash(t *testing.T) {
	db, done := authIntegrationDB(t)
	defer done()
	r := NewUserRepository(db)
	u1 := &entity.User{PhoneHash: "same", PasswordHash: "hash", AuthSalt: "salt", KdfSalt: "kdf", CreatedAt: time.Now(), UpdatedAt: time.Now()}
	require.NoError(t, r.Create(context.Background(), u1))
	u2 := &entity.User{PhoneHash: "same", PasswordHash: "hash2", AuthSalt: "salt2", KdfSalt: "kdf2", CreatedAt: time.Now(), UpdatedAt: time.Now()}
	err := r.Create(context.Background(), u2)
	assert.Equal(t, "CONFLICT", stringCode(err))
}

func TestRefreshTokenRepository_RevokeIsIdempotenceSafe(t *testing.T) {
	db, done := authIntegrationDB(t)
	defer done()
	r := NewRefreshTokenRepository(db)
	token := &entity.RefreshToken{TokenHash: "token-hash", UserID: "user-1", DeviceID: "device-1", CreatedAt: time.Now().UTC(), ExpiresAt: time.Now().UTC().Add(time.Hour)}
	require.NoError(t, r.Create(context.Background(), token))
	require.NoError(t, r.Revoke(context.Background(), token.TokenHash))
	err := r.Revoke(context.Background(), token.TokenHash)
	assert.Equal(t, "CONFLICT", stringCode(err))
	found, err := r.FindByHash(context.Background(), token.TokenHash)
	require.NoError(t, err)
	assert.NotNil(t, found.RevokedAt)
}

func TestRefreshTokenRepository_FindMissingReturnsNotFound(t *testing.T) {
	db, done := authIntegrationDB(t)
	defer done()
	r := NewRefreshTokenRepository(db)
	_, err := r.FindByHash(context.Background(), "missing")
	assert.Equal(t, "NOT_FOUND", stringCode(err))
}

func stringCode(err error) string {
	if err == nil {
		return ""
	}
	msg := err.Error()
	for i := 0; i < len(msg); i++ {
		if msg[i] == ':' {
			return msg[:i]
		}
	}
	return msg
}
