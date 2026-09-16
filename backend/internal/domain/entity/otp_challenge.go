package entity

import "time"

type OTPPurpose string

const (
	OTPPurposeRegister          OTPPurpose = "REGISTER"
	OTPPurposeLogin             OTPPurpose = "LOGIN"
	OTPPurposePasswordReset     OTPPurpose = "PASSWORD_RESET"
	OTPPurposeEmailVerification OTPPurpose = "EMAIL_VERIFICATION"
)

type OTPChallenge struct {
	ID              string
	UserID          string
	DestinationHash string
	Channel         string
	Purpose         OTPPurpose
	CodeHash        string
	CreatedAt       time.Time
	ExpiresAt       time.Time
	ConsumedAt      *time.Time
	Attempts        int
}
