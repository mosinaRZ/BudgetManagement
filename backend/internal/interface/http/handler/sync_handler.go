package handler

import (
	"encoding/base64"
	"net/http"
	"strconv"
	"strings"

	"github.com/go-playground/validator/v10"

	"github.com/mosinaRZ/finance-sync-backend/internal/domain/apperror"
	"github.com/mosinaRZ/finance-sync-backend/internal/interface/http/contextkeys"
	"github.com/mosinaRZ/finance-sync-backend/internal/interface/http/dto"
	"github.com/mosinaRZ/finance-sync-backend/internal/pkg/response"
	syncUsecase "github.com/mosinaRZ/finance-sync-backend/internal/usecase/sync"
)

type SyncHandler struct {
	usecase  *syncUsecase.SyncUsecase
	validate *validator.Validate
}

func NewSyncHandler(usecase *syncUsecase.SyncUsecase, validate *validator.Validate) *SyncHandler {
	if validate == nil {
		validate = validator.New()
	}
	return &SyncHandler{usecase: usecase, validate: validate}
}

func (h *SyncHandler) HandleSync(w http.ResponseWriter, r *http.Request) {
	if h.usecase == nil {
		response.Error(w, apperror.ErrInternal("sync service is not configured"))
		return
	}
	userID, ok := contextkeys.UserID(r.Context())
	if !ok || strings.TrimSpace(userID) == "" {
		response.Error(w, apperror.ErrUnauthorized("unauthorized access"))
		return
	}

	var req dto.SyncRequest
	if err := decodeJSON(w, r, &req); err != nil {
		response.Error(w, err)
		return
	}
	if err := validateRequest(h.validate, req); err != nil {
		response.Error(w, err)
		return
	}

	cursor := uint64(0)
	if strings.TrimSpace(req.Cursor) != "" {
		parsed, err := strconv.ParseUint(req.Cursor, 10, 64)
		if err != nil || parsed > uint64(^uint64(0)>>1) {
			response.Error(w, apperror.ErrValidation("invalid cursor"))
			return
		}
		cursor = parsed
	}

	input := syncUsecase.SyncInput{
		RequestID: req.RequestID,
		DeviceID:  req.DeviceID,
		Cursor:    cursor,
		Changes:   make([]syncUsecase.SyncChange, 0, len(req.Changes)),
	}
	for _, change := range req.Changes {
		mapped, err := dtoToUsecaseChange(change)
		if err != nil {
			response.Error(w, err)
			return
		}
		input.Changes = append(input.Changes, mapped)
	}

	result, err := h.usecase.Execute(r.Context(), userID, input)
	if err != nil {
		response.Error(w, err)
		return
	}
	response.JSON(w, http.StatusOK, usecaseToDTO(result))
}

func dtoToUsecaseChange(c dto.SyncChangeDTO) (syncUsecase.SyncChange, error) {
	ciphertext, err := base64.StdEncoding.DecodeString(c.Ciphertext)
	if err != nil {
		return syncUsecase.SyncChange{}, apperror.ErrValidation("ciphertext must be base64")
	}
	nonce, err := base64.StdEncoding.DecodeString(c.Nonce)
	if err != nil {
		return syncUsecase.SyncChange{}, apperror.ErrValidation("nonce must be base64")
	}
	return syncUsecase.SyncChange{EntityType: c.EntityType, EntityID: c.EntityID, Ciphertext: ciphertext, Nonce: nonce, Version: c.Version, IsDeleted: c.IsDeleted, UpdatedAt: c.UpdatedAt}, nil
}

func usecaseToDTO(result syncUsecase.SyncOutput) dto.SyncResponse {
	out := dto.SyncResponse{ServerChanges: make([]dto.SyncChangeDTO, 0, len(result.ServerChanges)), Conflicts: make([]dto.SyncChangeDTO, 0, len(result.Conflicts)), NextCursor: strconv.FormatUint(result.NextCursor, 10), HasMore: result.HasMore, SyncedAt: result.SyncedAt}
	for _, change := range result.ServerChanges {
		out.ServerChanges = append(out.ServerChanges, changeToDTO(change))
	}
	for _, change := range result.Conflicts {
		out.Conflicts = append(out.Conflicts, changeToDTO(change))
	}
	return out
}

func changeToDTO(c syncUsecase.SyncChange) dto.SyncChangeDTO {
	return dto.SyncChangeDTO{EntityType: c.EntityType, EntityID: c.EntityID, Ciphertext: base64.StdEncoding.EncodeToString(c.Ciphertext), Nonce: base64.StdEncoding.EncodeToString(c.Nonce), Version: c.Version, IsDeleted: c.IsDeleted, UpdatedAt: c.UpdatedAt}
}
