package auth

import (
	"crypto/aes"
	"crypto/cipher"
	"encoding/base64"
	"encoding/hex"
	"testing"
)

// Reference vectors were produced independently with Python (hashlib/hmac) and are asserted
// by the Android unit test CidnaKdfTest as well: both platforms must agree byte for byte.
func TestKeyScheduleMatchesReferenceVectors(t *testing.T) {
	salt := make([]byte, 16)
	for i := range salt {
		salt[i] = byte(i)
	}
	master := DeriveMasterKey("correct horse battery staple", salt)
	if got := hex.EncodeToString(master); got != "ef177144eec9420cbc1093d2a8b344a92bc506d0d4ec9c028dd19f8324d8c1e6" {
		t.Fatalf("master key mismatch: %s", got)
	}
	if got := AuthSecretFromMasterKey(master); got != "dDkCvX0jTBYzaVoxNjQPRh7k7YYnyd4jbM0Jg05nHOE=" {
		t.Fatalf("auth secret mismatch: %s", got)
	}
	if got := hex.EncodeToString(passwordWrapKey(master)); got != "a54c3fa5cac75136360d7f2bb0354b780251b27ae35065317b68f1f03feeb51b" {
		t.Fatalf("wrap key mismatch: %s", got)
	}

	recovery := make([]byte, 32)
	for i := range recovery {
		recovery[i] = byte(i + 32)
	}
	if got := hex.EncodeToString(recoveryWrapKey(recovery)); got != "f9a2d833c5f93e2bc5b8dd984f528e5748ba105ccb39c1bd98190e875dc4f436" {
		t.Fatalf("recovery wrap key mismatch: %s", got)
	}
	proof := RecoveryProofFromBytes(recovery)
	if proof != "0j7trEP4Yn8Eq97HkhJY17I8KZJvwlA/UWMr6X7iNp0=" {
		t.Fatalf("recovery proof mismatch: %s", proof)
	}
	proofBytes, _ := base64.StdEncoding.DecodeString(proof)
	if got := base64.StdEncoding.EncodeToString(RecoveryKeyHashFromProof(proofBytes)); got != "7F5vHTQGEXb8HfjmYR43WIsscYKILvfpCWqQjPg9IrE=" {
		t.Fatalf("recovery hash mismatch: %s", got)
	}
}

func TestCreateClientKeyMaterialMatchesEnvelopeContract(t *testing.T) {
	const password = "correct horse battery staple"
	m, err := CreateClientKeyMaterial(password)
	if err != nil {
		t.Fatal(err)
	}
	salt, err := base64.StdEncoding.DecodeString(m.KdfSalt)
	if err != nil || len(salt) != 16 {
		t.Fatalf("invalid KDF salt: %v", err)
	}
	envelope, err := base64.StdEncoding.DecodeString(m.PasswordEnvelope)
	if err != nil || len(envelope) != 48 {
		t.Fatalf("invalid password envelope: %v", err)
	}
	nonce, err := base64.StdEncoding.DecodeString(m.PasswordNonce)
	if err != nil || len(nonce) != 12 {
		t.Fatalf("invalid password nonce: %v", err)
	}
	recoveryHash, err := base64.StdEncoding.DecodeString(m.RecoveryKeyHash)
	if err != nil || len(recoveryHash) != 32 {
		t.Fatalf("invalid recovery hash: %v", err)
	}

	master := DeriveMasterKey(password, salt)
	if m.AuthSecret != AuthSecretFromMasterKey(master) {
		t.Fatal("auth secret does not follow from the password")
	}
	block, err := aes.NewCipher(passwordWrapKey(master))
	if err != nil {
		t.Fatal(err)
	}
	gcm, err := cipher.NewGCM(block)
	if err != nil {
		t.Fatal(err)
	}
	plaintext, err := gcm.Open(nil, nonce, envelope, []byte(aadPasswordEnvelope))
	if err != nil || len(plaintext) != 32 {
		t.Fatalf("password envelope is not decryptable: %v", err)
	}
	// The envelope is bound to its purpose and cannot be replayed as a recovery envelope.
	if _, err := gcm.Open(nil, nonce, envelope, []byte(aadRecoveryEnvelope)); err == nil {
		t.Fatal("envelope must not open under another purpose")
	}
}
