package handler

import (
	"encoding/json"
	"errors"
	"io"
	"net/http"

	"github.com/go-playground/validator/v10"

	"github.com/mosinaRZ/finance-sync-backend/internal/domain/apperror"
	"github.com/mosinaRZ/finance-sync-backend/internal/pkg/response"
)

const maxJSONBodyBytes = 5 * 1024 * 1024

func decodeJSON(w http.ResponseWriter, r *http.Request, dst any) error {
	r.Body = http.MaxBytesReader(w, r.Body, maxJSONBodyBytes)
	defer r.Body.Close()

	dec := json.NewDecoder(r.Body)
	dec.DisallowUnknownFields()
	if err := dec.Decode(dst); err != nil {
		if errors.Is(err, io.EOF) {
			return apperror.ErrValidation("request body is empty")
		}
		var maxErr *http.MaxBytesError
		if errors.As(err, &maxErr) {
			return apperror.ErrValidation("request body is too large")
		}
		return apperror.ErrValidation("invalid json body")
	}

	var extra any
	if err := dec.Decode(&extra); err != io.EOF {
		return apperror.ErrValidation("request body must contain exactly one json value")
	}
	return nil
}

func validateRequest(v *validator.Validate, value any) error {
	if v == nil {
		return apperror.ErrInternal("request validator is not configured")
	}
	if err := v.Struct(value); err != nil {
		return apperror.ErrValidation("validation failed")
	}
	return nil
}

func writeMethodNotAllowed(w http.ResponseWriter) {
	response.Error(w, apperror.ErrBadRequest("method not allowed"))
}
