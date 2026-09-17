package entity

import "time"

type AuditLog struct {
	ID           string
	ActorUserID  string
	TargetUserID string
	Action       string
	OldValue     string
	NewValue     string
	CreatedAt    time.Time
}
