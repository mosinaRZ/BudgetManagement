package dto

import "time"

type SyncChangeDTO struct {
	EntityType string    `json:"entityType" validate:"required,oneof=TRANSACTION CATEGORY BUDGET_LIMIT DEBT_CREDIT SAVING_GOAL NOTIFICATION PENDING_TRANSACTION"`
	EntityID   string    `json:"entityId" validate:"required,uuid,max=128"`
	Ciphertext string    `json:"ciphertext"`
	Nonce      string    `json:"nonce"`
	Version    int       `json:"version" validate:"required,min=1"`
	IsDeleted  bool      `json:"isDeleted"`
	UpdatedAt  time.Time `json:"updatedAt" validate:"required"`
}

type SyncRequest struct {
	RequestID string          `json:"requestId" validate:"required,max=128"`
	DeviceID  string          `json:"deviceId" validate:"required,max=128"`
	Cursor    string          `json:"cursor" validate:"max=32"`
	Changes   []SyncChangeDTO `json:"changes" validate:"max=100,dive"`
}

type SyncResponse struct {
	ServerChanges []SyncChangeDTO `json:"changes"`
	Conflicts     []SyncChangeDTO `json:"conflicts"`
	NextCursor    string          `json:"nextCursor"`
	HasMore       bool            `json:"hasMore"`
	SyncedAt      time.Time       `json:"syncedAt"`
}
