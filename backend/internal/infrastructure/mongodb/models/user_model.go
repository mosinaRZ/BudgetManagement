package models

import (
	"time"

	"go.mongodb.org/mongo-driver/bson/primitive"

	"github.com/mosinaRZ/finance-sync-backend/internal/domain/entity"
)

type UserModel struct {
	ID               primitive.ObjectID `bson:"_id,omitempty"`
	PhoneHash        string             `bson:"phoneHash"`
	PasswordHash     string             `bson:"passwordHash"`
	AuthSalt         string             `bson:"authSalt"`
	KdfSalt          string             `bson:"kdfSalt"`
	EncryptedProfile []byte             `bson:"encryptedProfile,omitempty"`
	ProfileNonce     []byte             `bson:"profileNonce,omitempty"`
	CreatedAt        time.Time          `bson:"createdAt"`
	UpdatedAt        time.Time          `bson:"updatedAt"`
	Devices          []string           `bson:"devices"`
}

func (m *UserModel) ToEntity() *entity.User {
	if m == nil {
		return nil
	}
	return &entity.User{ID: m.ID.Hex(), PhoneHash: m.PhoneHash, PasswordHash: m.PasswordHash, AuthSalt: m.AuthSalt, KdfSalt: m.KdfSalt,
		EncryptedProfile: append([]byte(nil), m.EncryptedProfile...), ProfileNonce: append([]byte(nil), m.ProfileNonce...),
		CreatedAt: m.CreatedAt, UpdatedAt: m.UpdatedAt, Devices: append([]string(nil), m.Devices...)}
}

func UserModelFromEntity(e *entity.User) *UserModel {
	m := &UserModel{PhoneHash: e.PhoneHash, PasswordHash: e.PasswordHash, AuthSalt: e.AuthSalt, KdfSalt: e.KdfSalt,
		EncryptedProfile: append([]byte(nil), e.EncryptedProfile...), ProfileNonce: append([]byte(nil), e.ProfileNonce...),
		CreatedAt: e.CreatedAt, UpdatedAt: e.UpdatedAt, Devices: append([]string(nil), e.Devices...)}
	if e.ID != "" {
		if id, err := primitive.ObjectIDFromHex(e.ID); err == nil {
			m.ID = id
		}
	}
	return m
}
