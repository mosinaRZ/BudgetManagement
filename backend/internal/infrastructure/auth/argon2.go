package auth

import (
	"crypto/rand"
	"crypto/subtle"
	"encoding/base64"
	"errors"

	"golang.org/x/crypto/argon2"
)

const (
	saltSize    = 16
	memory      = 64 * 1024
	iterations  = 3
	parallelism = 2
	keyLength   = 32
)

func GenerateSalt() ([]byte, error) {
	salt := make([]byte, saltSize)
	if _, err := rand.Read(salt); err != nil {
		return nil, errors.New("failed to generate random salt")
	}
	return salt, nil
}

func HashPassword(password string, salt []byte) (string, error) {
	if len(salt) < saltSize {
		return "", errors.New("password salt is too short")
	}
	hash := argon2.IDKey([]byte(password), salt, iterations, memory, parallelism, keyLength)
	return base64.RawStdEncoding.EncodeToString(hash), nil
}

func VerifyPassword(password string, saltBase64 string, storedHashBase64 string) (bool, error) {
	salt, err := base64.RawStdEncoding.DecodeString(saltBase64)
	if err != nil || len(salt) < saltSize {
		return false, errors.New("invalid password salt")
	}
	storedHash, err := base64.RawStdEncoding.DecodeString(storedHashBase64)
	if err != nil || len(storedHash) != keyLength {
		return false, errors.New("invalid password hash")
	}
	computedHash := argon2.IDKey([]byte(password), salt, iterations, memory, parallelism, keyLength)
	return subtle.ConstantTimeCompare(storedHash, computedHash) == 1, nil
}
