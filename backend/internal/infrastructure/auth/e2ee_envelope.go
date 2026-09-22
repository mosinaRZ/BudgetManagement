package auth

import (
	"crypto/aes"
	"crypto/cipher"
	"crypto/rand"
	"crypto/sha256"
	"encoding/base64"
	"errors"
	"fmt"

	"golang.org/x/crypto/pbkdf2"
)

const ClientPBKDF2Iterations = 120_000

// ClientKeyMaterial matches the Android SyncKeyManager envelope format:
// PBKDF2-HMAC-SHA256 -> AES-256-GCM, 16-byte KDF salt and 12-byte GCM nonces.
type ClientKeyMaterial struct {
	KdfSalt          string
	PasswordEnvelope string
	PasswordNonce    string
	RecoveryKey      string
	RecoveryKeyHash  string
	RecoveryEnvelope string
	RecoveryNonce    string
}

func CreateClientKeyMaterial(password string) (ClientKeyMaterial, error) {
	if password == "" {
		return ClientKeyMaterial{}, errors.New("password is required")
	}
	salt := make([]byte, 16)
	if _, err := rand.Read(salt); err != nil {
		return ClientKeyMaterial{}, fmt.Errorf("generate KDF salt: %w", err)
	}
	dataKey := make([]byte, 32)
	if _, err := rand.Read(dataKey); err != nil {
		return ClientKeyMaterial{}, fmt.Errorf("generate data key: %w", err)
	}
	passwordKey := pbkdf2.Key([]byte(password), salt, ClientPBKDF2Iterations, 32, sha256.New)
	passwordEnvelope, passwordNonce, err := sealClientKey(passwordKey, dataKey)
	if err != nil {
		return ClientKeyMaterial{}, err
	}

	recoveryBytes := make([]byte, 32)
	if _, err := rand.Read(recoveryBytes); err != nil {
		return ClientKeyMaterial{}, fmt.Errorf("generate recovery key: %w", err)
	}
	recoveryKey := base64.RawStdEncoding.EncodeToString(recoveryBytes)
	recoveryHash := sha256.Sum256(recoveryBytes)
	recoveryWrappingKey := pbkdf2.Key([]byte(recoveryKey), salt, ClientPBKDF2Iterations, 32, sha256.New)
	recoveryEnvelope, recoveryNonce, err := sealClientKey(recoveryWrappingKey, dataKey)
	if err != nil {
		return ClientKeyMaterial{}, err
	}

	return ClientKeyMaterial{
		KdfSalt:          base64.RawStdEncoding.EncodeToString(salt),
		PasswordEnvelope: base64.RawStdEncoding.EncodeToString(passwordEnvelope),
		PasswordNonce:    base64.RawStdEncoding.EncodeToString(passwordNonce),
		RecoveryKey:      recoveryKey,
		RecoveryKeyHash:  base64.RawStdEncoding.EncodeToString(recoveryHash[:]),
		RecoveryEnvelope: base64.RawStdEncoding.EncodeToString(recoveryEnvelope),
		RecoveryNonce:    base64.RawStdEncoding.EncodeToString(recoveryNonce),
	}, nil
}

func sealClientKey(key, plaintext []byte) ([]byte, []byte, error) {
	block, err := aes.NewCipher(key)
	if err != nil {
		return nil, nil, fmt.Errorf("create envelope cipher: %w", err)
	}
	gcm, err := cipher.NewGCM(block)
	if err != nil {
		return nil, nil, fmt.Errorf("create envelope GCM: %w", err)
	}
	nonce := make([]byte, gcm.NonceSize())
	if _, err := rand.Read(nonce); err != nil {
		return nil, nil, fmt.Errorf("generate envelope nonce: %w", err)
	}
	return gcm.Seal(nil, nonce, plaintext, nil), nonce, nil
}
