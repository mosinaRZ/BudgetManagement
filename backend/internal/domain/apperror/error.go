// Package apperror defines the application's transport-independent error categories.
// HTTP status mapping belongs to the HTTP interface layer.
package apperror

import (
	"errors"
	"fmt"
)

type Code string

const (
	CodeValidation   Code = "VALIDATION_ERROR"
	CodeBadRequest   Code = "BAD_REQUEST"
	CodeUnauthorized Code = "UNAUTHORIZED"
	CodeForbidden    Code = "FORBIDDEN"
	CodeNotFound     Code = "NOT_FOUND"
	CodeConflict     Code = "CONFLICT"
	CodeRateLimited  Code = "RATE_LIMITED"
	CodeInternal     Code = "INTERNAL_ERROR"
)

type AppError struct {
	Code    Code
	Message string
	Err     error
}

func (e *AppError) Error() string {
	if e == nil {
		return "<nil>"
	}
	if e.Err != nil {
		return fmt.Sprintf("%s: %s: %v", e.Code, e.Message, e.Err)
	}
	return fmt.Sprintf("%s: %s", e.Code, e.Message)
}

func (e *AppError) Unwrap() error { return e.Err }

func (e *AppError) Is(target error) bool {
	t, ok := target.(*AppError)
	return ok && e != nil && t != nil && e.Code == t.Code
}

func newAppError(code Code, message string, wrapped error) *AppError {
	return &AppError{Code: code, Message: message, Err: wrapped}
}

func ErrValidation(message string, wrapped ...error) *AppError {
	return newAppError(CodeValidation, message, firstOrNil(wrapped))
}
func ErrBadRequest(message string, wrapped ...error) *AppError {
	return newAppError(CodeBadRequest, message, firstOrNil(wrapped))
}
func ErrUnauthorized(message string, wrapped ...error) *AppError {
	return newAppError(CodeUnauthorized, message, firstOrNil(wrapped))
}
func ErrForbidden(message string, wrapped ...error) *AppError {
	return newAppError(CodeForbidden, message, firstOrNil(wrapped))
}
func ErrNotFound(message string, wrapped ...error) *AppError {
	return newAppError(CodeNotFound, message, firstOrNil(wrapped))
}
func ErrConflict(message string, wrapped ...error) *AppError {
	return newAppError(CodeConflict, message, firstOrNil(wrapped))
}
func ErrRateLimited(message string, wrapped ...error) *AppError {
	return newAppError(CodeRateLimited, message, firstOrNil(wrapped))
}
func ErrInternal(message string, wrapped ...error) *AppError {
	return newAppError(CodeInternal, message, firstOrNil(wrapped))
}

func firstOrNil(errs []error) error {
	if len(errs) == 0 {
		return nil
	}
	return errs[0]
}

func CodeOf(err error) (Code, bool) {
	var appErr *AppError
	if !errors.As(err, &appErr) || appErr == nil {
		return "", false
	}
	return appErr.Code, true
}
