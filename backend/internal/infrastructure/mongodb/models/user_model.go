package models

import (
	"time"

	"github.com/mosinaRZ/finance-sync-backend/internal/domain/entity"
	"go.mongodb.org/mongo-driver/bson/primitive"
)

type UserModel struct {
	ID                  primitive.ObjectID `bson:"_id,omitempty"`
	Role                entity.Role        `bson:"role"`
	PhoneHash           string             `bson:"phoneHash,omitempty"`
	PhoneNumber         string             `bson:"phoneNumber,omitempty"`
	PhoneVerified       bool               `bson:"phoneVerified"`
	FirstName           string             `bson:"firstName,omitempty"`
	LastName            string             `bson:"lastName,omitempty"`
	Gender              entity.Gender      `bson:"gender,omitempty"`
	BirthDate           string             `bson:"birthDate,omitempty"`
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
	FailedLoginAttempts int                `bson:"failedLoginAttempts"`
	LockedUntil         *time.Time         `bson:"lockedUntil,omitempty"`
	SessionVersion      uint64             `bson:"sessionVersion,omitempty"`
	// IsDeleted is stored explicitly (no omitempty) so "isDeleted: false" is queryable;
	// documents written before this field existed decode as false (active).
	IsDeleted bool       `bson:"isDeleted"`
	DeletedAt *time.Time `bson:"deletedAt,omitempty"`
}

func (m *UserModel) ToEntity() *entity.User {
	if m == nil {
		return nil
	}
	role := m.Role
	if !role.Valid() {
		role = entity.RoleUser
	}
	return &entity.User{ID: m.ID.Hex(), Role: role, PhoneHash: m.PhoneHash, PhoneNumber: m.PhoneNumber, PhoneVerified: m.PhoneVerified, FirstName: m.FirstName, LastName: m.LastName, Gender: m.Gender, BirthDate: m.BirthDate, PasswordHash: m.PasswordHash, AuthSalt: m.AuthSalt, KdfSalt: m.KdfSalt, PasswordKeyEnvelope: append([]byte(nil), m.PasswordKeyEnvelope...), PasswordKeyNonce: append([]byte(nil), m.PasswordKeyNonce...), RecoveryKeyHash: m.RecoveryKeyHash, RecoveryKeyEnvelope: append([]byte(nil), m.RecoveryKeyEnvelope...), RecoveryKeyNonce: append([]byte(nil), m.RecoveryKeyNonce...), CreatedAt: m.CreatedAt, UpdatedAt: m.UpdatedAt, Devices: append([]string(nil), m.Devices...), FailedLoginAttempts: m.FailedLoginAttempts, LockedUntil: m.LockedUntil, SessionVersion: m.SessionVersion, IsDeleted: m.IsDeleted, DeletedAt: m.DeletedAt}
}
func UserModelFromEntity(e *entity.User) *UserModel {
	role := e.Role
	if !role.Valid() {
		role = entity.RoleUser
	}
	m := &UserModel{Role: role, PhoneHash: e.PhoneHash, PhoneNumber: e.PhoneNumber, PhoneVerified: e.PhoneVerified, FirstName: e.FirstName, LastName: e.LastName, Gender: e.Gender, BirthDate: e.BirthDate, PasswordHash: e.PasswordHash, AuthSalt: e.AuthSalt, KdfSalt: e.KdfSalt, PasswordKeyEnvelope: append([]byte(nil), e.PasswordKeyEnvelope...), PasswordKeyNonce: append([]byte(nil), e.PasswordKeyNonce...), RecoveryKeyHash: e.RecoveryKeyHash, RecoveryKeyEnvelope: append([]byte(nil), e.RecoveryKeyEnvelope...), RecoveryKeyNonce: append([]byte(nil), e.RecoveryKeyNonce...), CreatedAt: e.CreatedAt, UpdatedAt: e.UpdatedAt, Devices: append([]string(nil), e.Devices...), FailedLoginAttempts: e.FailedLoginAttempts, LockedUntil: e.LockedUntil, SessionVersion: e.SessionVersion, IsDeleted: e.IsDeleted, DeletedAt: e.DeletedAt}
	if e.ID != "" {
		if id, err := primitive.ObjectIDFromHex(e.ID); err == nil {
			m.ID = id
		}
	}
	return m
}
