package response

import (
	"encoding/json"
	"errors"
	"log/slog"
	"net/http"

	"github.com/mosinaRZ/finance-sync-backend/internal/domain/apperror"
)

type ErrorResponse struct {
	Error ErrorDetail `json:"error"`
}

type ErrorDetail struct {
	Code    string `json:"code"`
	Message string `json:"message"`
}

func JSON(w http.ResponseWriter, status int, data any) {
	w.Header().Set("Content-Type", "application/json")
	w.WriteHeader(status)
	if data != nil {
		_ = json.NewEncoder(w).Encode(data)
	}
}

func Error(w http.ResponseWriter, err error) {
	var appErr *apperror.AppError
	if errors.As(err, &appErr) && appErr != nil {
		JSON(w, statusForCode(appErr.Code), ErrorResponse{Error: ErrorDetail{
			Code:    string(appErr.Code),
			Message: appErr.Message,
		}})
		return
	}

	if err != nil {
		slog.Error("Unhandled error occurred", "error", err)
	}
	JSON(w, http.StatusInternalServerError, ErrorResponse{Error: ErrorDetail{
		Code:    string(apperror.CodeInternal),
		Message: "An unexpected internal server error occurred",
	}})
}

// These helpers keep transport-specific status mapping at the HTTP boundary.
func ErrNotFound(message string) error         { return apperror.ErrNotFound(message) }
func ErrMethodNotAllowed(message string) error { return apperror.ErrBadRequest(message) }

func statusForCode(code apperror.Code) int {
	switch code {
	case apperror.CodeValidation, apperror.CodeBadRequest:
		return http.StatusBadRequest
	case apperror.CodeUnauthorized:
		return http.StatusUnauthorized
	case apperror.CodeForbidden:
		return http.StatusForbidden
	case apperror.CodeNotFound:
		return http.StatusNotFound
	case apperror.CodeConflict:
		return http.StatusConflict
	case apperror.CodeRateLimited:
		return http.StatusTooManyRequests
	default:
		return http.StatusInternalServerError
	}
}
