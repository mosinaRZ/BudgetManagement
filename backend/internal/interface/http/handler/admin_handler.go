package handler

import (
	"net/http"

	"github.com/go-chi/chi/v5"
	"github.com/go-playground/validator/v10"

	"github.com/mosinaRZ/finance-sync-backend/internal/domain/apperror"
	"github.com/mosinaRZ/finance-sync-backend/internal/domain/entity"
	"github.com/mosinaRZ/finance-sync-backend/internal/interface/http/contextkeys"
	"github.com/mosinaRZ/finance-sync-backend/internal/interface/http/dto"
	"github.com/mosinaRZ/finance-sync-backend/internal/pkg/response"
	adminusecase "github.com/mosinaRZ/finance-sync-backend/internal/usecase/admin"
)

type AdminHandler struct {
	roles    adminusecase.RoleService
	validate *validator.Validate
}

func NewAdminHandler(roles adminusecase.RoleService, v *validator.Validate) *AdminHandler {
	if v == nil {
		v = validator.New()
	}
	return &AdminHandler{roles: roles, validate: v}
}

func (h *AdminHandler) UpdateUserRole(w http.ResponseWriter, r *http.Request) {
	var q dto.UpdateUserRoleRequest
	if err := decodeJSON(w, r, &q); err != nil {
		response.Error(w, err)
		return
	}
	if err := validateRequest(h.validate, q); err != nil {
		response.Error(w, err)
		return
	}
	userID := chi.URLParam(r, "userID")
	actorID, ok := contextkeys.UserID(r.Context())
	if !ok || actorID == "" {
		response.Error(w, apperror.ErrUnauthorized("authentication required"))
		return
	}
	if err := h.roles.UpdateUserRole(r.Context(), actorID, userID, entity.Role(q.Role)); err != nil {
		response.Error(w, err)
		return
	}
	response.JSON(w, http.StatusOK, dto.UpdateUserRoleResponse{UserID: userID, Role: q.Role})
}
