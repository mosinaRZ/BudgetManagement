package auth

import (
	"context"
	"crypto/rand"
	"crypto/subtle"
	"encoding/base64"
	"errors"
	"sync/atomic"
	"time"

	"golang.org/x/crypto/argon2"
)

const (
	saltSize    = 16
	memory      = 64 * 1024 // KiB: every in-flight hash holds 64 MiB
	iterations  = 3
	parallelism = 2
	keyLength   = 32
)

// Argon2 admission control.
//
// Each hash allocates 64 MiB, and login/registration/password-reset run it before the caller is
// authenticated, so an attacker with many source IPs could start dozens at once and exhaust the
// memory of a small server. All hashes therefore pass through one gate: at most maxConcurrent run
// at the same time, a bounded number may wait for a slot for a limited time, and everything else
// is refused with ErrBusy (surfaced as HTTP 429) instead of being queued without bound.
const (
	DefaultArgon2MaxConcurrent = 2
	DefaultArgon2MaxWaiting    = 64
	DefaultArgon2MaxWait       = 5 * time.Second
)

// ErrBusy means the hashing capacity is exhausted. It says nothing about the credentials, so
// callers must never count it as a failed login attempt.
var ErrBusy = errors.New("password hashing capacity exhausted")

type argon2Gate struct {
	slots      chan struct{}
	waiting    atomic.Int64
	maxWaiting int64
	maxWait    time.Duration
}

var gate atomic.Pointer[argon2Gate]

func init() {
	ConfigureArgon2(DefaultArgon2MaxConcurrent, DefaultArgon2MaxWaiting, DefaultArgon2MaxWait)
}

// ConfigureArgon2 sets how many hashes may run at once, how many requests may wait for a free
// slot, and for how long. Non-positive values fall back to the defaults. Call it once at startup,
// before serving requests.
func ConfigureArgon2(maxConcurrent, maxWaiting int, maxWait time.Duration) {
	if maxConcurrent <= 0 {
		maxConcurrent = DefaultArgon2MaxConcurrent
	}
	if maxWaiting < 0 {
		maxWaiting = DefaultArgon2MaxWaiting
	}
	if maxWait <= 0 {
		maxWait = DefaultArgon2MaxWait
	}
	gate.Store(&argon2Gate{slots: make(chan struct{}, maxConcurrent), maxWaiting: int64(maxWaiting), maxWait: maxWait})
}

// acquire reserves a hashing slot and returns the function that releases it.
func (g *argon2Gate) acquire(ctx context.Context) (func(), error) {
	release := func() { <-g.slots }
	if err := ctx.Err(); err != nil {
		return nil, err
	}
	select {
	case g.slots <- struct{}{}:
		return release, nil
	default:
	}
	if g.waiting.Add(1) > g.maxWaiting {
		g.waiting.Add(-1)
		return nil, ErrBusy
	}
	defer g.waiting.Add(-1)
	timer := time.NewTimer(g.maxWait)
	defer timer.Stop()
	select {
	case g.slots <- struct{}{}:
		return release, nil
	case <-timer.C:
		return nil, ErrBusy
	case <-ctx.Done():
		return nil, ctx.Err()
	}
}

// ReserveArgon2Slot reserves one hashing slot until the returned function is called. It exists for
// tests and for tools that need to simulate or hold back hashing capacity; normal code never
// needs it, because HashPasswordContext and VerifyPasswordContext reserve a slot themselves.
func ReserveArgon2Slot(ctx context.Context) (release func(), err error) {
	return gate.Load().acquire(ctx)
}

func GenerateSalt() ([]byte, error) {
	salt := make([]byte, saltSize)
	if _, err := rand.Read(salt); err != nil {
		return nil, errors.New("failed to generate random salt")
	}
	return salt, nil
}

// HashPassword is HashPasswordContext without cancellation (maintenance tools, package init).
func HashPassword(password string, salt []byte) (string, error) {
	return HashPasswordContext(context.Background(), password, salt)
}

// HashPasswordContext hashes under the shared admission gate. It returns ErrBusy when the server
// has no capacity, or the context's error if the caller gave up while waiting.
func HashPasswordContext(ctx context.Context, password string, salt []byte) (string, error) {
	if len(salt) < saltSize {
		return "", errors.New("password salt is too short")
	}
	release, err := gate.Load().acquire(ctx)
	if err != nil {
		return "", err
	}
	defer release()
	hash := argon2.IDKey([]byte(password), salt, iterations, memory, parallelism, keyLength)
	return base64.RawStdEncoding.EncodeToString(hash), nil
}

// VerifyPassword is VerifyPasswordContext without cancellation.
func VerifyPassword(password string, saltBase64 string, storedHashBase64 string) (bool, error) {
	return VerifyPasswordContext(context.Background(), password, saltBase64, storedHashBase64)
}

// VerifyPasswordContext verifies under the shared admission gate. A non-nil error that is
// ErrBusy or a context error means "could not check", NOT "wrong password": callers must not
// record a failed login for it.
func VerifyPasswordContext(ctx context.Context, password string, saltBase64 string, storedHashBase64 string) (bool, error) {
	salt, err := base64.RawStdEncoding.DecodeString(saltBase64)
	if err != nil || len(salt) < saltSize {
		return false, errors.New("invalid password salt")
	}
	storedHash, err := base64.RawStdEncoding.DecodeString(storedHashBase64)
	if err != nil || len(storedHash) != keyLength {
		return false, errors.New("invalid password hash")
	}
	release, err := gate.Load().acquire(ctx)
	if err != nil {
		return false, err
	}
	defer release()
	computedHash := argon2.IDKey([]byte(password), salt, iterations, memory, parallelism, keyLength)
	return subtle.ConstantTimeCompare(storedHash, computedHash) == 1, nil
}

// IsCapacityError reports whether err means the check could not run (server busy or the caller
// went away) rather than that the credentials are wrong.
func IsCapacityError(err error) bool {
	return errors.Is(err, ErrBusy) || errors.Is(err, context.Canceled) || errors.Is(err, context.DeadlineExceeded)
}
