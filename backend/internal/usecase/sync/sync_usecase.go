package sync

import (
	"context"
	"errors"
	"time"

	"github.com/mosinaRZ/finance-sync-backend/internal/domain/apperror"
	"github.com/mosinaRZ/finance-sync-backend/internal/domain/entity"
	"github.com/mosinaRZ/finance-sync-backend/internal/domain/repository"
)

const MaxChangesPerRequest = 100

const (
	MaxDeviceIDLength        = 128
	MaxEntityTypeLength      = 64
	MaxEntityIDLength        = 128
	MaxCiphertextSize        = 1024 * 1024
	MaxNonceSize             = 64
	MaxRequestCiphertextSize = 4 * 1024 * 1024
)

type SyncUsecase struct {
	repo    repository.SyncRepository
	devices repository.DeviceRepository
}

func NewSyncUsecase(repo repository.SyncRepository, devices ...repository.DeviceRepository) *SyncUsecase {
	var d repository.DeviceRepository
	if len(devices) > 0 {
		d = devices[0]
	}
	return &SyncUsecase{repo: repo, devices: d}
}

func (u *SyncUsecase) Execute(ctx context.Context, userID string, req SyncInput) (SyncOutput, error) {
	if userID == "" || req.RequestID == "" || req.DeviceID == "" {
		return SyncOutput{}, apperror.ErrValidation("invalid sync identity")
	}
	if u.devices != nil {
		ok, err := u.devices.Exists(ctx, userID, req.DeviceID)
		if err != nil {
			return SyncOutput{}, err
		}
		if !ok {
			return SyncOutput{}, apperror.ErrForbidden("device is not registered")
		}
		_ = u.devices.Touch(ctx, userID, req.DeviceID)
	}
	if len(req.Changes) > MaxChangesPerRequest {
		return SyncOutput{}, apperror.ErrValidation("too many sync changes")
	}
	if len(req.DeviceID) > MaxDeviceIDLength || len(req.RequestID) > 128 {
		return SyncOutput{}, apperror.ErrValidation("sync identifier is too long")
	}

	records := make([]*entity.SyncRecord, 0, len(req.Changes))
	totalCiphertextSize := 0
	for _, change := range req.Changes {
		totalCiphertextSize += len(change.Ciphertext)
		if totalCiphertextSize > MaxRequestCiphertextSize || len(change.EntityType) > MaxEntityTypeLength || len(change.EntityID) > MaxEntityIDLength || len(change.Ciphertext) > MaxCiphertextSize || len(change.Nonce) > MaxNonceSize {
			return SyncOutput{}, apperror.ErrValidation("sync change exceeds size limits")
		}
		rec := &entity.SyncRecord{UserID: userID, EntityType: entity.EntityType(change.EntityType), EntityID: change.EntityID, Ciphertext: append([]byte(nil), change.Ciphertext...), Nonce: append([]byte(nil), change.Nonce...), Version: change.Version, IsDeleted: change.IsDeleted, UpdatedAt: change.UpdatedAt, DeviceID: req.DeviceID}
		if err := rec.Validate(); err != nil {
			return SyncOutput{}, apperror.ErrValidation(err.Error())
		}
		records = append(records, rec)
	}

	result, err := u.repo.Sync(ctx, repository.SyncRequest{RequestID: req.RequestID, UserID: userID, DeviceID: req.DeviceID, Cursor: repository.SyncCursor(req.Cursor), Records: records})
	if err != nil {
		var appErr *apperror.AppError
		if errors.As(err, &appErr) {
			return SyncOutput{}, err
		}
		return SyncOutput{}, apperror.ErrInternal("failed to synchronize records", err)
	}

	out := SyncOutput{ServerChanges: make([]SyncChange, 0, len(result.ServerChanges)), Conflicts: make([]SyncChange, 0, len(result.Conflicts)), NextCursor: uint64(result.NextCursor), HasMore: result.HasMore, SyncedAt: time.Now().UTC()}
	for _, r := range result.ServerChanges {
		out.ServerChanges = append(out.ServerChanges, mapEntityToChange(r))
	}
	for _, r := range result.Conflicts {
		out.Conflicts = append(out.Conflicts, mapEntityToChange(r))
	}
	return out, nil
}

func mapEntityToChange(e *entity.SyncRecord) SyncChange {
	return SyncChange{EntityType: string(e.EntityType), EntityID: e.EntityID, Ciphertext: append([]byte(nil), e.Ciphertext...), Nonce: append([]byte(nil), e.Nonce...), Version: e.Version, IsDeleted: e.IsDeleted, UpdatedAt: e.UpdatedAt, ServerRevision: e.ServerRevision}
}
