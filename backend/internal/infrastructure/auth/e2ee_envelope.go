package auth

import (
	"crypto/aes"
	"crypto/cipher"
	"crypto/hmac"
	"crypto/rand"
	"crypto/sha256"
	"encoding/base64"
	"errors"
	"fmt"

	"golang.org/x/crypto/pbkdf2"
	"golang.org/x/text/unicode/norm"
)

// Cidna key schedule, protocol v2 ("the password never leaves the device").
//
//	masterKey  = PBKDF2-HMAC-SHA256(NFKC(password), kdfSalt, 600 000, 32 bytes)
//	authSecret = HKDF-Expand(masterKey, "cidna/v2/authentication")  -> the only thing sent to the server
//	wrapKey    = HKDF-Expand(masterKey, "cidna/v2/password-wrap")   -> never leaves the device
//
// This file is the Go twin of the Android client's CidnaKdf.kt. It exists so operator
// tooling (seed-admin) and tests produce byte-identical material; the HTTP server itself
// never derives any of these keys from a password.
const (
	ClientPBKDF2Iterations = 600_000
	KDFVersion             = 2
	KDFSaltBytes           = 16

	infoAuth            = "cidna/v2/authentication"
	infoPasswordWrap    = "cidna/v2/password-wrap"
	infoRecoveryWrap    = "cidna/v2/recovery-wrap"
	infoRecoveryProof   = "cidna/v2/recovery-proof"
	aadPasswordEnvelope = "cidna/v2/password-envelope"
	aadRecoveryEnvelope = "cidna/v2/recovery-envelope"
)

// ClientKeyMaterial mirrors what the Android app sends at registration. All binary values are
// padded standard Base64 (what the app produces); AuthSecret is the value sent as auth_secret.
type ClientKeyMaterial struct {
	KdfSalt          string
	AuthSecret       string
	PasswordEnvelope string
	PasswordNonce    string
	RecoveryKey      string
	RecoveryKeyHash  string
	RecoveryEnvelope string
	RecoveryNonce    string
}

// DeriveMasterKey derives the account master key from a password (NFKC-normalised).
func DeriveMasterKey(password string, salt []byte) []byte {
	normalized := norm.NFKC.String(password)
	return pbkdf2.Key([]byte(normalized), salt, ClientPBKDF2Iterations, 32, sha256.New)
}

// hkdfExpand32 is HKDF-Expand (RFC 5869) for a single 32-byte block with HMAC-SHA256.
func hkdfExpand32(prk []byte, info string) []byte {
	mac := hmac.New(sha256.New, prk)
	mac.Write([]byte(info))
	mac.Write([]byte{0x01})
	return mac.Sum(nil)
}

// AuthSecretFromMasterKey returns the base64 value a client sends as auth_secret.
func AuthSecretFromMasterKey(masterKey []byte) string {
	return base64.StdEncoding.EncodeToString(hkdfExpand32(masterKey, infoAuth))
}

func passwordWrapKey(masterKey []byte) []byte { return hkdfExpand32(masterKey, infoPasswordWrap) }
func recoveryWrapKey(recovery []byte) []byte  { return hkdfExpand32(recovery, infoRecoveryWrap) }

// RecoveryProofFromBytes is what a client sends as recovery_proof.
func RecoveryProofFromBytes(recovery []byte) string {
	return base64.StdEncoding.EncodeToString(hkdfExpand32(recovery, infoRecoveryProof))
}

// RecoveryKeyHashFromProof is what the server stores: SHA-256 of the raw proof bytes.
func RecoveryKeyHashFromProof(proof []byte) []byte {
	h := sha256.Sum256(proof)
	return h[:]
}

func CreateClientKeyMaterial(password string) (ClientKeyMaterial, error) {
	if password == "" {
		return ClientKeyMaterial{}, errors.New("password is required")
	}
	salt := make([]byte, KDFSaltBytes)
	if _, err := rand.Read(salt); err != nil {
		return ClientKeyMaterial{}, fmt.Errorf("generate KDF salt: %w", err)
	}
	dataKey := make([]byte, 32)
	if _, err := rand.Read(dataKey); err != nil {
		return ClientKeyMaterial{}, fmt.Errorf("generate data key: %w", err)
	}
	masterKey := DeriveMasterKey(password, salt)
	passwordEnvelope, passwordNonce, err := sealClientKey(passwordWrapKey(masterKey), dataKey, aadPasswordEnvelope)
	if err != nil {
		return ClientKeyMaterial{}, err
	}

	recoveryBytes := make([]byte, 32)
	if _, err := rand.Read(recoveryBytes); err != nil {
		return ClientKeyMaterial{}, fmt.Errorf("generate recovery key: %w", err)
	}
	recoveryEnvelope, recoveryNonce, err := sealClientKey(recoveryWrapKey(recoveryBytes), dataKey, aadRecoveryEnvelope)
	if err != nil {
		return ClientKeyMaterial{}, err
	}
	proof := hkdfExpand32(recoveryBytes, infoRecoveryProof)

	enc := base64.StdEncoding
	return ClientKeyMaterial{
		KdfSalt:          enc.EncodeToString(salt),
		AuthSecret:       AuthSecretFromMasterKey(masterKey),
		PasswordEnvelope: enc.EncodeToString(passwordEnvelope),
		PasswordNonce:    enc.EncodeToString(passwordNonce),
		RecoveryKey:      enc.EncodeToString(recoveryBytes),
		RecoveryKeyHash:  enc.EncodeToString(RecoveryKeyHashFromProof(proof)),
		RecoveryEnvelope: enc.EncodeToString(recoveryEnvelope),
		RecoveryNonce:    enc.EncodeToString(recoveryNonce),
	}, nil
}

func sealClientKey(key, plaintext []byte, aad string) ([]byte, []byte, error) {
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
	return gcm.Seal(nil, nonce, plaintext, []byte(aad)), nonce, nil
}
