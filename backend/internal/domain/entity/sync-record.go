package entity

import (
	"fmt"
	"time"
)

type EntityType string

const (
	EntityTypeTransaction         EntityType = "TRANSACTION"
	EntityTypeCategory            EntityType = "CATEGORY"
	EntityTypeBudgetLimit         EntityType = "BUDGET_LIMIT"
	EntityTypeDebtCredit          EntityType = "DEBT_CREDIT"
	EntityTypeSavingGoal          EntityType = "SAVING_GOAL"
	EntityTypeSavingGoalOperation EntityType = "SAVING_GOAL_OPERATION"
	EntityTypeDebtPayment         EntityType = "DEBT_PAYMENT"
)

var validEntityTypes = map[EntityType]struct{}{
	EntityTypeTransaction: {}, EntityTypeCategory: {}, EntityTypeBudgetLimit: {},
	EntityTypeDebtCredit: {}, EntityTypeSavingGoal: {}, EntityTypeSavingGoalOperation: {}, EntityTypeDebtPayment: {},
}

func (e EntityType) IsValid() bool { _, ok := validEntityTypes[e]; return ok }

type SyncRecord struct {
	ID             string
	UserID         string
	EntityType     EntityType
	EntityID       string
	Ciphertext     []byte
	Nonce          []byte
	Version        int
	UpdatedAt      time.Time
	IsDeleted      bool
	DeviceID       string
	ServerRevision uint64
}

func (s *SyncRecord) Validate() error {
	if s == nil {
		return fmt.Errorf("sync record: record is required")
	}
	if s.UserID == "" {
		return fmt.Errorf("sync record: user id is required")
	}
	if s.EntityID == "" {
		return fmt.Errorf("sync record: entity id is required")
	}
	if !s.EntityType.IsValid() {
		return fmt.Errorf("sync record: invalid entity type %q", s.EntityType)
	}
	if s.Version < 1 {
		return fmt.Errorf("sync record: version must be at least 1")
	}
	if !s.IsDeleted {
		if len(s.Ciphertext) == 0 {
			return fmt.Errorf("sync record: ciphertext is required for non-deleted records")
		}
		if len(s.Nonce) == 0 {
			return fmt.Errorf("sync record: nonce is required for non-deleted records")
		}
	}
	return nil
}
