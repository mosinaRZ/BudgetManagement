package sync

import "time"

type SyncChange struct {
	EntityType     string
	EntityID       string
	Ciphertext     []byte
	Nonce          []byte
	Version        int
	IsDeleted      bool
	UpdatedAt      time.Time
	ServerRevision uint64
}

type SyncInput struct {
	RequestID string
	DeviceID  string
	Cursor    uint64
	Changes   []SyncChange
}

type SyncOutput struct {
	ServerChanges []SyncChange
	Conflicts     []SyncChange
	NextCursor    uint64
	HasMore       bool
	SyncedAt      time.Time
}
