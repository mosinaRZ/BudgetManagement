package auth

import (
	"errors"
	"strings"
)

// MinKeyLength is the minimum length of every server-side key.
const MinKeyLength = 32

// Keys holds the independent server-side secrets of the authentication flow.
//
// Each key has exactly one purpose, so leaking or rotating one never affects the others:
//
//   - JWTSecret signs access tokens. Rotating it only signs everybody out.
//   - PhoneHashKey is the HMAC key behind the stored phone identifier (users.phoneHash and the
//     OTP destination hash). Rotating it detaches existing accounts from their identifier until
//     the stored hashes are recomputed (see cmd/rehash-phones).
//   - OTPCodeKey is the HMAC key behind stored one-time-code hashes. Rotating it only invalidates
//     codes that are currently in flight.
//   - DecoySaltKey derives the deterministic fake KDF salt returned for unknown identifiers.
//     Rotating it changes those decoys, which is harmless.
//
// The keys must be different values: reusing one value for two purposes recreates the coupling
// this type exists to remove.
type Keys struct {
	JWTSecret    string
	PhoneHashKey string
	OTPCodeKey   string
	DecoySaltKey string
}

// Validate reports whether every key is present, long enough and distinct from the others.
func (k Keys) Validate() error {
	named := []struct{ name, value string }{
		{"JWT secret", k.JWTSecret},
		{"phone hash key", k.PhoneHashKey},
		{"OTP code key", k.OTPCodeKey},
		{"decoy salt key", k.DecoySaltKey},
	}
	seen := make(map[string]string, len(named))
	for _, n := range named {
		v := strings.TrimSpace(n.value)
		if v == "" {
			return errors.New(n.name + " is not configured")
		}
		if len(v) < MinKeyLength {
			return errors.New(n.name + " is too short")
		}
		if other, dup := seen[v]; dup {
			return errors.New(n.name + " must differ from the " + other)
		}
		seen[v] = n.name
	}
	return nil
}
