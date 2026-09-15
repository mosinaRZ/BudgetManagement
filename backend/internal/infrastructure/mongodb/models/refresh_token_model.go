package models

import (
	"time"

	"go.mongodb.org/mongo-driver/bson/primitive"

	"github.com/mosinaRZ/finance-sync-backend/internal/domain/entity"
)

type RefreshTokenModel struct {
	ID        primitive.ObjectID `bson:"_id,omitempty"`
	TokenHash string             `bson:"tokenHash"`
	UserID    string             `bson:"userId"`
	DeviceID  string             `bson:"deviceId"`
	CreatedAt time.Time          `bson:"createdAt"`
	ExpiresAt time.Time          `bson:"expiresAt"`
	RevokedAt *time.Time         `bson:"revokedAt,omitempty"`
}

func (m *RefreshTokenModel) ToEntity() *entity.RefreshToken {
	if m == nil {
		return nil
	}
	return &entity.RefreshToken{ID: m.ID.Hex(), TokenHash: m.TokenHash, UserID: m.UserID, DeviceID: m.DeviceID,
		CreatedAt: m.CreatedAt, ExpiresAt: m.ExpiresAt, RevokedAt: m.RevokedAt}
}

func RefreshTokenModelFromEntity(e *entity.RefreshToken) *RefreshTokenModel {
	m := &RefreshTokenModel{TokenHash: e.TokenHash, UserID: e.UserID, DeviceID: e.DeviceID, CreatedAt: e.CreatedAt, ExpiresAt: e.ExpiresAt, RevokedAt: e.RevokedAt}
	if e.ID != "" {
		if id, err := primitive.ObjectIDFromHex(e.ID); err == nil {
			m.ID = id
		}
	}
	return m
}
