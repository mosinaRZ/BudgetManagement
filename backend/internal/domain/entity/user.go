package entity

import "time"

// User represents an account holder of the finance management app.
// The server never sees plaintext user data: PasswordHash/AuthSalt/KdfSalt
// are used only for authentication, while EncryptedProfile holds the
// client-side End-to-End Encrypted profile blob (ciphertext), decryptable
// only by the client that holds the encryption key.
type User struct {
	// ID is the unique identifier of the user (server-generated).
	ID string

	// PhoneHash is a one-way hash of the user's phone number, used as the
	// lookup key for authentication instead of storing the raw phone number.
	PhoneHash string

	// PasswordHash is the hash of the user's authentication password/PIN,
	// used purely for login and unrelated to the E2E encryption key.
	PasswordHash string

	// AuthSalt is the salt used when hashing the authentication credential.
	AuthSalt string

	// KdfSalt is the salt used client-side to derive the E2E encryption key
	// from the user's secret (e.g. via PBKDF2/Argon2). The server stores it
	// only so the client can re-derive the same key across devices.
	KdfSalt string

	// EncryptedProfile is the ciphertext of the user's profile data,
	// encrypted client-side. The server cannot decrypt it.
	EncryptedProfile []byte

	// ProfileNonce is the nonce/IV used to encrypt EncryptedProfile.
	ProfileNonce []byte

	// CreatedAt is the timestamp the account was created.
	CreatedAt time.Time

	// UpdatedAt is the timestamp the account was last modified.
	UpdatedAt time.Time

	// Devices is the list of device IDs currently associated with this user.
	Devices []string
}
