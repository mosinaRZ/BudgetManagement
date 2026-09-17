package handler

import (
	"github.com/go-chi/chi/v5"
	"github.com/mosinaRZ/finance-sync-backend/internal/domain/apperror"
	"github.com/mosinaRZ/finance-sync-backend/internal/interface/http/contextkeys"
	"github.com/mosinaRZ/finance-sync-backend/internal/interface/http/dto"
	"github.com/mosinaRZ/finance-sync-backend/internal/pkg/response"
	deviceusecase "github.com/mosinaRZ/finance-sync-backend/internal/usecase/device"
	"net/http"
)

type DeviceHandler struct{ service *deviceusecase.Service }

func NewDeviceHandler(service *deviceusecase.Service) *DeviceHandler {
	return &DeviceHandler{service: service}
}

func (h *DeviceHandler) List(w http.ResponseWriter, r *http.Request) {
	userID, ok := contextkeys.UserID(r.Context())
	if !ok || userID == "" {
		response.Error(w, apperror.ErrUnauthorized("authentication required"))
		return
	}
	devices, err := h.service.List(r.Context(), userID)
	if err != nil {
		response.Error(w, err)
		return
	}
	out := make([]dto.DeviceResponse, 0, len(devices))
	for _, d := range devices {
		out = append(out, dto.DeviceResponse{ID: d.ID, LastSeenAt: d.LastSeenAt.Unix(), CreatedAt: d.CreatedAt.Unix()})
	}
	response.JSON(w, http.StatusOK, dto.DeviceListResponse{Devices: out})
}

func (h *DeviceHandler) Revoke(w http.ResponseWriter, r *http.Request) {
	userID, ok := contextkeys.UserID(r.Context())
	if !ok || userID == "" {
		response.Error(w, apperror.ErrUnauthorized("authentication required"))
		return
	}
	if err := h.service.Revoke(r.Context(), userID, chi.URLParam(r, "deviceID")); err != nil {
		response.Error(w, err)
		return
	}
	w.WriteHeader(http.StatusNoContent)
}
