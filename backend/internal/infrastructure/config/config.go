package config

import (
	"fmt"
	"os"
	"strconv"
	"strings"
	"time"

	"github.com/joho/godotenv"
)

type Config struct {
	Env, HTTPPort, MongoURI, MongoDBName, JWTSecret string
	// Independent server-side keys. JWTSecret only signs access tokens; each of the keys below
	// has a single purpose (see usecase/auth.Keys) and must be a different value.
	PhoneHashKey, OTPCodeKey, DecoySaltKey string
	// Resource limits. Zero means "use the built-in default".
	//
	// Argon2MaxConcurrent bounds simultaneous password hashes (each holds 64 MiB) and
	// Argon2MaxWaiting how many more may queue for a slot before requests are refused.
	// SyncMaxRecords / SyncMaxBytes are the per-account storage quota (see repository.SyncQuota).
	Argon2MaxConcurrent, Argon2MaxWaiting  int
	SyncMaxRecords, SyncMaxBytes           int64
	JWTAccessTTL, JWTRefreshTTL, OTPExpiry time.Duration
	SMSWebhookURL, SMSAuthToken            string
	SMSFromNumber, SMSPatternCode          string
	DevLogOTP                              bool
	TrustedProxyCIDRs, CORSAllowedOrigins  string
	TLSCertFile, TLSKeyFile                string
	// TLSTerminatedByProxy means a reverse proxy on the same host (e.g. Caddy)
	// terminates HTTPS and the API itself listens on plain HTTP.
	// ServeTLS is derived: true only when the API must terminate TLS itself.
	TLSTerminatedByProxy, ServeTLS bool
	// MongoAllowPlaintext allows a non-TLS MONGO_URI in production. Intended only
	// for MongoDB reachable exclusively over a private/loopback network (single VPS).
	MongoAllowPlaintext bool
}

func Load() (*Config, error) {
	_ = godotenv.Load()
	env := strings.ToLower(strings.TrimSpace(get("ENV", "dev")))
	switch env {
	case "development":
		env = "dev"
	case "production":
		env = "prod"
	}
	if env != "dev" && env != "test" && env != "prod" {
		return nil, fmt.Errorf("ENV must be one of: dev, test, prod")
	}
	c := &Config{Env: env, HTTPPort: os.Getenv("HTTP_PORT"), MongoURI: secretValue("MONGO_URI"), MongoDBName: os.Getenv("MONGO_DB_NAME"), JWTSecret: secretValue("JWT_SECRET"), PhoneHashKey: secretValue("PHONE_HASH_KEY"), OTPCodeKey: secretValue("OTP_HMAC_KEY"), DecoySaltKey: secretValue("LOGIN_DECOY_KEY"), SMSWebhookURL: os.Getenv("SMS_WEBHOOK_URL"), SMSAuthToken: secretValue("SMS_AUTH_TOKEN"), SMSFromNumber: strings.TrimSpace(os.Getenv("SMS_FROM_NUMBER")), SMSPatternCode: strings.TrimSpace(os.Getenv("SMS_PATTERN_CODE")), TrustedProxyCIDRs: os.Getenv("TRUSTED_PROXY_CIDRS"), CORSAllowedOrigins: os.Getenv("CORS_ALLOWED_ORIGINS"), TLSCertFile: os.Getenv("TLS_CERT_FILE"), TLSKeyFile: os.Getenv("TLS_KEY_FILE"), DevLogOTP: strings.EqualFold(get("DEV_LOG_OTP", "false"), "true"), TLSTerminatedByProxy: envBool("TLS_TERMINATED_BY_PROXY"), MongoAllowPlaintext: envBool("MONGO_ALLOW_PLAINTEXT")}
	for _, k := range []string{"HTTP_PORT", "MONGO_DB_NAME", "JWT_ACCESS_TTL_MINUTES", "JWT_REFRESH_TTL_DAYS"} {
		if strings.TrimSpace(os.Getenv(k)) == "" {
			return nil, fmt.Errorf("configuration incomplete: missing %s", k)
		}
	}
	if c.MongoURI == "" {
		return nil, fmt.Errorf("configuration incomplete: missing MONGO_URI or MONGO_URI_FILE")
	}
	if c.JWTSecret == "" {
		return nil, fmt.Errorf("configuration incomplete: missing JWT_SECRET or JWT_SECRET_FILE")
	}
	a, e := pos("JWT_ACCESS_TTL_MINUTES", os.Getenv("JWT_ACCESS_TTL_MINUTES"))
	if e != nil {
		return nil, e
	}
	d, e := pos("JWT_REFRESH_TTL_DAYS", os.Getenv("JWT_REFRESH_TTL_DAYS"))
	if e != nil {
		return nil, e
	}
	o := 15
	if v := os.Getenv("OTP_EXPIRY_MINUTES"); v != "" {
		o, e = pos("OTP_EXPIRY_MINUTES", v)
		if e != nil {
			return nil, e
		}
	}
	for _, l := range []struct {
		env string
		dst *int
		max int
	}{{"ARGON2_MAX_CONCURRENCY", &c.Argon2MaxConcurrent, 64}, {"ARGON2_MAX_WAITING", &c.Argon2MaxWaiting, 100000}} {
		if v := strings.TrimSpace(os.Getenv(l.env)); v != "" {
			n, err := pos(l.env, v)
			if err != nil {
				return nil, err
			}
			if n > l.max {
				return nil, fmt.Errorf("%s must be at most %d", l.env, l.max)
			}
			*l.dst = n
		}
	}
	for _, l := range []struct {
		env string
		dst *int64
	}{{"SYNC_MAX_RECORDS_PER_ACCOUNT", &c.SyncMaxRecords}, {"SYNC_MAX_BYTES_PER_ACCOUNT", &c.SyncMaxBytes}} {
		if v := strings.TrimSpace(os.Getenv(l.env)); v != "" {
			n, err := strconv.ParseInt(v, 10, 64)
			if err != nil || n <= 0 {
				return nil, fmt.Errorf("%s must be a positive integer", l.env)
			}
			*l.dst = n
		}
	}
	port, e := strconv.Atoi(os.Getenv("HTTP_PORT"))
	if e != nil || port < 1 || port > 65535 {
		return nil, fmt.Errorf("invalid HTTP_PORT")
	}
	if len(c.JWTSecret) < 32 {
		return nil, fmt.Errorf("JWT_SECRET must be at least 32 characters")
	}
	if err := validateKeys(c, 32); err != nil {
		return nil, err
	}
	if a > 24*60 {
		return nil, fmt.Errorf("JWT_ACCESS_TTL_MINUTES must be at most 1440")
	}
	if d > 365 {
		return nil, fmt.Errorf("JWT_REFRESH_TTL_DAYS must be at most 365")
	}
	if o > 60 {
		return nil, fmt.Errorf("OTP_EXPIRY_MINUTES must be at most 60")
	}
	if !strings.EqualFold(c.Env, "dev") && !strings.EqualFold(c.Env, "test") && c.DevLogOTP {
		return nil, fmt.Errorf("DEV_LOG_OTP can only be enabled in dev or test")
	}
	if c.Env == "prod" {
		if len(c.JWTSecret) < 64 {
			return nil, fmt.Errorf("JWT_SECRET must be at least 64 characters in production")
		}
		if err := validateKeys(c, 64); err != nil {
			return nil, err
		}
		if c.TLSTerminatedByProxy {
			// HTTPS is terminated by a reverse proxy on the same host. Without a trusted
			// proxy CIDR every client would appear as the proxy's IP and share one
			// rate-limit bucket (and forwarded headers would never be honoured).
			if strings.TrimSpace(c.TrustedProxyCIDRs) == "" {
				return nil, fmt.Errorf("TRUSTED_PROXY_CIDRS is required in production when TLS_TERMINATED_BY_PROXY=true")
			}
		} else {
			if strings.TrimSpace(c.TLSCertFile) == "" || strings.TrimSpace(c.TLSKeyFile) == "" {
				return nil, fmt.Errorf("TLS_CERT_FILE and TLS_KEY_FILE are required in production (or set TLS_TERMINATED_BY_PROXY=true when a reverse proxy terminates HTTPS)")
			}
			if _, err := os.Stat(c.TLSCertFile); err != nil {
				return nil, fmt.Errorf("TLS certificate file is not readable: %w", err)
			}
			if _, err := os.Stat(c.TLSKeyFile); err != nil {
				return nil, fmt.Errorf("TLS key file is not readable: %w", err)
			}
			c.ServeTLS = true
		}
		mongoURI := strings.ToLower(strings.TrimSpace(c.MongoURI))
		if !strings.HasPrefix(mongoURI, "mongodb+srv://") && !strings.Contains(mongoURI, "tls=true") && !c.MongoAllowPlaintext {
			return nil, fmt.Errorf("MONGO_URI must use TLS in production (set MONGO_ALLOW_PLAINTEXT=true only when MongoDB is reachable solely over a private network, e.g. the same-host Docker network)")
		}
		if strings.TrimSpace(c.SMSWebhookURL) == "" {
			return nil, fmt.Errorf("SMS_WEBHOOK_URL is required in production")
		}
		if c.SMSAuthToken == "" || c.SMSFromNumber == "" || c.SMSPatternCode == "" {
			return nil, fmt.Errorf("SMS_AUTH_TOKEN, SMS_FROM_NUMBER and SMS_PATTERN_CODE are required in production")
		}
	}
	return &Config{Env: c.Env, HTTPPort: c.HTTPPort, MongoURI: c.MongoURI, MongoDBName: c.MongoDBName, JWTSecret: c.JWTSecret, PhoneHashKey: c.PhoneHashKey, OTPCodeKey: c.OTPCodeKey, DecoySaltKey: c.DecoySaltKey, Argon2MaxConcurrent: c.Argon2MaxConcurrent, Argon2MaxWaiting: c.Argon2MaxWaiting, SyncMaxRecords: c.SyncMaxRecords, SyncMaxBytes: c.SyncMaxBytes, TrustedProxyCIDRs: c.TrustedProxyCIDRs, CORSAllowedOrigins: c.CORSAllowedOrigins, TLSCertFile: c.TLSCertFile, TLSKeyFile: c.TLSKeyFile, TLSTerminatedByProxy: c.TLSTerminatedByProxy, ServeTLS: c.ServeTLS, MongoAllowPlaintext: c.MongoAllowPlaintext, JWTAccessTTL: time.Duration(a) * time.Minute, JWTRefreshTTL: time.Duration(d) * 24 * time.Hour, OTPExpiry: time.Duration(o) * time.Minute, SMSWebhookURL: c.SMSWebhookURL, SMSAuthToken: c.SMSAuthToken, SMSFromNumber: c.SMSFromNumber, SMSPatternCode: c.SMSPatternCode, DevLogOTP: c.DevLogOTP}, nil
}

// validateKeys checks the dedicated keys: all present, at least minLen characters, and pairwise
// different from each other and from JWT_SECRET. Reusing one value for two purposes would
// recreate the coupling that the separate keys exist to remove.
func validateKeys(c *Config, minLen int) error {
	keys := []struct{ env, value, hint string }{
		{"JWT_SECRET", c.JWTSecret, ""},
		{"PHONE_HASH_KEY", c.PhoneHashKey, " (to keep existing accounts, set it to the value JWT_SECRET had before the keys were split, then give JWT_SECRET a new value)"},
		{"OTP_HMAC_KEY", c.OTPCodeKey, ""},
		{"LOGIN_DECOY_KEY", c.DecoySaltKey, ""},
	}
	seen := make(map[string]string, len(keys))
	for _, k := range keys {
		if k.value == "" {
			return fmt.Errorf("configuration incomplete: missing %s or %s_FILE%s", k.env, k.env, k.hint)
		}
		if len(k.value) < minLen {
			return fmt.Errorf("%s must be at least %d characters", k.env, minLen)
		}
		if other, dup := seen[k.value]; dup {
			return fmt.Errorf("%s must not have the same value as %s: every key needs its own independent random value", k.env, other)
		}
		seen[k.value] = k.env
	}
	return nil
}

func secretValue(name string) string {
	if v := strings.TrimSpace(os.Getenv(name)); v != "" {
		return v
	}
	file := strings.TrimSpace(os.Getenv(name + "_FILE"))
	if file == "" {
		return ""
	}
	b, err := os.ReadFile(file)
	if err != nil {
		return ""
	}
	return strings.TrimSpace(string(b))
}

func envBool(k string) bool {
	return strings.EqualFold(strings.TrimSpace(os.Getenv(k)), "true")
}

func get(k, d string) string {
	if v := os.Getenv(k); v != "" {
		return v
	}
	return d
}
func pos(k, v string) (int, error) {
	n, e := strconv.Atoi(strings.TrimSpace(v))
	if e != nil || n <= 0 {
		return 0, fmt.Errorf("%s must be positive", k)
	}
	return n, nil
}
