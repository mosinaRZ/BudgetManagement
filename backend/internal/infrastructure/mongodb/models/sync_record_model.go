package models

import (
	"errors"
	"time"

	"github.com/mosinaRZ/finance-sync-backend/internal/domain/entity"
	"go.mongodb.org/mongo-driver/bson/primitive"
)

type SyncRecordModel struct {
	ID                   primitive.ObjectID `bson:"_id,omitempty"`
	UserID               string             `bson:"userId"`
	EntityType           string             `bson:"entityType"`
	EntityID             string             `bson:"entityId"`
	Ciphertext           []byte             `bson:"ciphertext"`
	Nonce                []byte             `bson:"nonce"`
	Version              int                `bson:"version"`
	UpdatedAt            time.Time          `bson:"updatedAt"`
	IsDeleted            bool               `bson:"isDeleted"`
	DeviceID             string             `bson:"deviceId"`
	ServerRevision       uint64             `bson:"serverRevision"`
	EncryptionKeyVersion int                `bson:"encryptionKeyVersion"`
}

func (m *SyncRecordModel) ToEntity() *entity.SyncRecord {
	if m == nil {
		return nil
	}
	keyVersion := m.EncryptionKeyVersion
	if keyVersion < 1 {
		keyVersion = 1
	}
	return &entity.SyncRecord{ID: m.ID.Hex(), UserID: m.UserID, EntityType: entity.EntityType(m.EntityType), EntityID: m.EntityID, Ciphertext: append([]byte(nil), m.Ciphertext...), Nonce: append([]byte(nil), m.Nonce...), Version: m.Version, UpdatedAt: m.UpdatedAt, IsDeleted: m.IsDeleted, DeviceID: m.DeviceID, ServerRevision: m.ServerRevision, EncryptionKeyVersion: keyVersion}
}

func FromEntity(e *entity.SyncRecord) (*SyncRecordModel, error) {
	if e == nil {
		return nil, errors.New("sync record model: entity is nil")
	}
	m := &SyncRecordModel{UserID: e.UserID, EntityType: string(e.EntityType), EntityID: e.EntityID, Ciphertext: append([]byte(nil), e.Ciphertext...), Nonce: append([]byte(nil), e.Nonce...), Version: e.Version, UpdatedAt: e.UpdatedAt, IsDeleted: e.IsDeleted, DeviceID: e.DeviceID, ServerRevision: e.ServerRevision, EncryptionKeyVersion: e.EncryptionKeyVersion}
	if e.ID != "" {
		oid, err := primitive.ObjectIDFromHex(e.ID)
		if err != nil {
			return nil, err
		}
		m.ID = oid
	}
	return m, nil
}
