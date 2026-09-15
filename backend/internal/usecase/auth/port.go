package auth

import "context"

type RegisterInput struct {
	PhoneNumber string
	Password    string
	DeviceID    string
}
type RegisterOutput struct {
	AccessToken  string
	RefreshToken string
	KdfSalt      string
	UserID       string
}
type LoginInput struct {
	PhoneNumber string
	Password    string
	DeviceID    string
}
type LoginOutput struct {
	AccessToken  string
	RefreshToken string
	KdfSalt      string
	UserID       string
}
type RefreshInput struct{ RefreshToken string }
type RefreshOutput struct {
	AccessToken  string
	RefreshToken string
}

type Service interface {
	Register(context.Context, RegisterInput) (RegisterOutput, error)
	Login(context.Context, LoginInput) (LoginOutput, error)
	Refresh(context.Context, RefreshInput) (RefreshOutput, error)
}
