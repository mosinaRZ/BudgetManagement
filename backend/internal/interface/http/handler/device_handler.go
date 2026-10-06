package handler

import (
	"fmt"
	"net"
	"net/http"

	"github.com/go-chi/chi/v5"
	"github.com/mosinaRZ/finance-sync-backend/internal/domain/apperror"
	"github.com/mosinaRZ/finance-sync-backend/internal/interface/http/contextkeys"
	"github.com/mosinaRZ/finance-sync-backend/internal/interface/http/dto"
	"github.com/mosinaRZ/finance-sync-backend/internal/pkg/response"
	deviceusecase "github.com/mosinaRZ/finance-sync-backend/internal/usecase/device"
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
	overview, err := h.service.Overview(r.Context(), userID)
	if err != nil {
		response.Error(w, err)
		return
	}
	out := make([]dto.DeviceResponse, 0, len(overview.Devices))
	for _, d := range overview.Devices {
		out = append(out, dto.DeviceResponse{
			ID: d.ID, Name: d.Name, Model: d.Model, Platform: d.Platform, OSVersion: d.OSVersion, AppVersion: d.AppVersion,
			LastIP:     maskIP(d.LastIP),
			LastSeenAt: d.LastSeenAt.Unix(), CreatedAt: d.CreatedAt.Unix(),
			IsPrimary: overview.PrimaryID != "" && d.ID == overview.PrimaryID,
		})
	}
	response.JSON(w, http.StatusOK, dto.DeviceListResponse{Devices: out})
}

func (h *DeviceHandler) Revoke(w http.ResponseWriter, r *http.Request) {
	userID, ok := contextkeys.UserID(r.Context())
	if !ok || userID == "" {
		response.Error(w, apperror.ErrUnauthorized("authentication required"))
		return
	}
	// Empty for legacy tokens issued before access tokens carried the device id.
	callerDeviceID, _ := contextkeys.DeviceID(r.Context())
	if err := h.service.Revoke(r.Context(), userID, callerDeviceID, chi.URLParam(r, "deviceID")); err != nil {
		response.Error(w, err)
		return
	}
	w.WriteHeader(http.StatusNoContent)
}

// maskIP hides the host part of an address so the device list never exposes a full IP:
// IPv4 keeps the first three octets, IPv6 keeps the first three groups. Anything that
// does not parse as an IP yields an empty string.
func maskIP(raw string) string {
	ip := net.ParseIP(raw)
	if ip == nil {
		return ""
	}
	if v4 := ip.To4(); v4 != nil {
		return fmt.Sprintf("%d.%d.%d.*", v4[0], v4[1], v4[2])
	}
	v6 := ip.To16()
	if v6 == nil {
		return ""
	}
	return fmt.Sprintf("%x:%x:%x:*", uint16(v6[0])<<8|uint16(v6[1]), uint16(v6[2])<<8|uint16(v6[3]), uint16(v6[4])<<8|uint16(v6[5]))
}
