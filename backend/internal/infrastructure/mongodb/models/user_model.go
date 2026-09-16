package models

import (
	"github.com/mosinaRZ/finance-sync-backend/internal/domain/entity"
	"go.mongodb.org/mongo-driver/bson/primitive"
	"time"
)

type UserModel struct {
	ID                  primitive.ObjectID `bson:"_id,omitempty"`
	PhoneHash           string             `bson:"phoneHash,omitempty"`
	EmailHash           string             `bson:"emailHash,omitempty"`
	PhoneVerified       bool               `bson:"phoneVerified"`
	EmailVerified       bool               `bson:"emailVerified"`
	PasswordHash        string             `bson:"passwordHash"`
	AuthSalt            string             `bson:"authSalt"`
	KdfSalt             string             `bson:"kdfSalt"`
	PasswordKeyEnvelope []byte             `bson:"passwordKeyEnvelope,omitempty"`
	PasswordKeyNonce    []byte             `bson:"passwordKeyNonce,omitempty"`
	RecoveryKeyHash     string             `bson:"recoveryKeyHash,omitempty"`
	RecoveryKeyEnvelope []byte             `bson:"recoveryKeyEnvelope,omitempty"`
	RecoveryKeyNonce    []byte             `bson:"recoveryKeyNonce,omitempty"`
	CreatedAt           time.Time          `bson:"createdAt"`
	UpdatedAt           time.Time          `bson:"updatedAt"`
	Devices             []string           `bson:"devices"`
}

func (m *UserModel) ToEntity() *entity.User {
	if m == nil {
		return nil
	}
	return &entity.User{ID: m.ID.Hex(), PhoneHash: m.PhoneHash, EmailHash: m.EmailHash, PhoneVerified: m.PhoneVerified, EmailVerified: m.EmailVerified, PasswordHash: m.PasswordHash, AuthSalt: m.AuthSalt, KdfSalt: m.KdfSalt, PasswordKeyEnvelope: append([]byte(nil), m.PasswordKeyEnvelope...), PasswordKeyNonce: append([]byte(nil), m.PasswordKeyNonce...), RecoveryKeyHash: m.RecoveryKeyHash, RecoveryKeyEnvelope: append([]byte(nil), m.RecoveryKeyEnvelope...), RecoveryKeyNonce: append([]byte(nil), m.RecoveryKeyNonce...), CreatedAt: m.CreatedAt, UpdatedAt: m.UpdatedAt, Devices: append([]string(nil), m.Devices...)}
}
func UserModelFromEntity(e *entity.User) *UserModel {
	m := &UserModel{PhoneHash: e.PhoneHash, EmailHash: e.EmailHash, PhoneVerified: e.PhoneVerified, EmailVerified: e.EmailVerified, PasswordHash: e.PasswordHash, AuthSalt: e.AuthSalt, KdfSalt: e.KdfSalt, PasswordKeyEnvelope: append([]byte(nil), e.PasswordKeyEnvelope...), PasswordKeyNonce: append([]byte(nil), e.PasswordKeyNonce...), RecoveryKeyHash: e.RecoveryKeyHash, RecoveryKeyEnvelope: append([]byte(nil), e.RecoveryKeyEnvelope...), RecoveryKeyNonce: append([]byte(nil), e.RecoveryKeyNonce...), CreatedAt: e.CreatedAt, UpdatedAt: e.UpdatedAt, Devices: append([]string(nil), e.Devices...)}
	if e.ID != "" {
		if id, err := primitive.ObjectIDFromHex(e.ID); err == nil {
			m.ID = id
		}
	}
	return m
}
