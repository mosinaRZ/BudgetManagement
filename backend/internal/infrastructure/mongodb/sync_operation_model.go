package mongodb

import (
	"github.com/mosinaRZ/finance-sync-backend/internal/infrastructure/mongodb/models"
	"time"
)

type syncOperationModel struct {
	UserID        string                   `bson:"userId"`
	RequestID     string                   `bson:"requestId"`
	Fingerprint   string                   `bson:"fingerprint"`
	NextCursor    int64                    `bson:"nextCursor"`
	HasMore       bool                     `bson:"hasMore"`
	ServerChanges []models.SyncRecordModel `bson:"serverChanges"`
	Conflicts     []models.SyncRecordModel `bson:"conflicts"`
	CreatedAt     time.Time                `bson:"createdAt"`
}
