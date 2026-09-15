package dto

type RegisterRequest struct {
	PhoneNumber string `json:"phone_number" validate:"required"`
	Password    string `json:"password" validate:"required,min=8,max=256"`
	DeviceID    string `json:"device_id" validate:"required,max=128"`
}

type RegisterResponse struct {
	AccessToken  string `json:"access_token"`
	RefreshToken string `json:"refresh_token"`
	KdfSalt      string `json:"kdf_salt"` // برای کلاینت (مشتق‌سازی کلید E2EE)
}

type LoginRequest struct {
	PhoneNumber string `json:"phone_number" validate:"required"`
	Password    string `json:"password" validate:"required,max=256"`
	DeviceID    string `json:"device_id" validate:"required,max=128"`
}

type LoginResponse struct {
	AccessToken  string `json:"access_token"`
	RefreshToken string `json:"refresh_token"`
	KdfSalt      string `json:"kdf_salt"`
}

type RefreshRequest struct {
	RefreshToken string `json:"refresh_token" validate:"required"`
}

type RefreshResponse struct {
	AccessToken  string `json:"access_token"`
	RefreshToken string `json:"refresh_token"`
}
