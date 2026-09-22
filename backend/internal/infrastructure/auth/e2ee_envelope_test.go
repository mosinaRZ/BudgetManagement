package auth

import (
	"crypto/aes"
	"crypto/cipher"
	"crypto/sha256"
	"encoding/base64"
	"testing"

	"golang.org/x/crypto/pbkdf2"
)

func TestCreateClientKeyMaterialMatchesEnvelopeContract(t *testing.T) {
	m, err := CreateClientKeyMaterial("correct horse battery staple")
	if err != nil {
		t.Fatal(err)
	}
	salt, err := base64.RawStdEncoding.DecodeString(m.KdfSalt)
	if err != nil || len(salt) != 16 {
		t.Fatalf("invalid KDF salt: %v", err)
	}
	envelope, err := base64.RawStdEncoding.DecodeString(m.PasswordEnvelope)
	if err != nil || len(envelope) != 48 {
		t.Fatalf("invalid password envelope: %v", err)
	}
	nonce, err := base64.RawStdEncoding.DecodeString(m.PasswordNonce)
	if err != nil || len(nonce) != 12 {
		t.Fatalf("invalid password nonce: %v", err)
	}
	recoveryHash, err := base64.RawStdEncoding.DecodeString(m.RecoveryKeyHash)
	if err != nil || len(recoveryHash) != 32 {
		t.Fatalf("invalid recovery hash: %v", err)
	}

	key := pbkdf2.Key([]byte("correct horse battery staple"), salt, ClientPBKDF2Iterations, 32, sha256.New)
	block, err := aes.NewCipher(key)
	if err != nil {
		t.Fatal(err)
	}
	gcm, err := cipher.NewGCM(block)
	if err != nil {
		t.Fatal(err)
	}
	plaintext, err := gcm.Open(nil, nonce, envelope, nil)
	if err != nil || len(plaintext) != 32 {
		t.Fatalf("password envelope is not decryptable: %v", err)
	}
}
