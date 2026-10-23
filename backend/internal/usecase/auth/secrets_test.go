package auth

// Authentication secrets are what clients send instead of passwords (see
// infrastructure/auth/e2ee_envelope.go): 32 bytes, base64. Tests use fixed, valid values.
const (
	testAuthSecret  = "AQIDBAUGBwgJCgsMDQ4PEBESExQVFhcYGRobHB0eHyA="
	testWrongSecret = "ISIjJCUmJygpKissLS4vMDEyMzQ1Njc4OTo7PD0+P0A="
)
