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
	Env, HTTPPort, MongoURI, MongoDBName, JWTSecret                                    string
	JWTAccessTTL, JWTRefreshTTL, OTPExpiry                                             time.Duration
	SMSWebhookURL, SMSAuthToken, SMTPHost, SMTPPort, SMTPUser, SMTPPassword, EmailFrom string
	DevLogOTP                                                                          bool
	RedisURL, TrustedProxyCIDRs, CORSAllowedOrigins                                    string
	TLSCertFile, TLSKeyFile                                                            string
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
	c := &Config{Env: env, HTTPPort: os.Getenv("HTTP_PORT"), MongoURI: secretValue("MONGO_URI"), MongoDBName: os.Getenv("MONGO_DB_NAME"), JWTSecret: secretValue("JWT_SECRET"), SMSWebhookURL: os.Getenv("SMS_WEBHOOK_URL"), SMSAuthToken: secretValue("SMS_AUTH_TOKEN"), SMTPHost: os.Getenv("SMTP_HOST"), SMTPPort: get("SMTP_PORT", "587"), SMTPUser: os.Getenv("SMTP_USER"), SMTPPassword: secretValue("SMTP_PASSWORD"), EmailFrom: os.Getenv("EMAIL_FROM"), RedisURL: secretValue("REDIS_URL"), TrustedProxyCIDRs: os.Getenv("TRUSTED_PROXY_CIDRS"), CORSAllowedOrigins: os.Getenv("CORS_ALLOWED_ORIGINS"), TLSCertFile: os.Getenv("TLS_CERT_FILE"), TLSKeyFile: os.Getenv("TLS_KEY_FILE"), DevLogOTP: strings.EqualFold(get("DEV_LOG_OTP", "false"), "true")}
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
	port, e := strconv.Atoi(os.Getenv("HTTP_PORT"))
	if e != nil || port < 1 || port > 65535 {
		return nil, fmt.Errorf("invalid HTTP_PORT")
	}
	if len(c.JWTSecret) < 32 {
		return nil, fmt.Errorf("JWT_SECRET must be at least 32 characters")
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
		if strings.TrimSpace(c.TLSCertFile) == "" || strings.TrimSpace(c.TLSKeyFile) == "" {
			return nil, fmt.Errorf("TLS_CERT_FILE and TLS_KEY_FILE are required in production")
		}
		if _, err := os.Stat(c.TLSCertFile); err != nil {
			return nil, fmt.Errorf("TLS certificate file is not readable: %w", err)
		}
		if _, err := os.Stat(c.TLSKeyFile); err != nil {
			return nil, fmt.Errorf("TLS key file is not readable: %w", err)
		}
		mongoURI := strings.ToLower(strings.TrimSpace(c.MongoURI))
		if !strings.HasPrefix(mongoURI, "mongodb+srv://") && !strings.Contains(mongoURI, "tls=true") {
			return nil, fmt.Errorf("MONGO_URI must use TLS in production")
		}
		if strings.TrimSpace(c.RedisURL) == "" {
			return nil, fmt.Errorf("REDIS_URL is required in production for distributed rate limiting")
		}
		if strings.TrimSpace(c.SMSWebhookURL) == "" {
			return nil, fmt.Errorf("SMS_WEBHOOK_URL is required in production")
		}
		if strings.TrimSpace(c.SMTPHost) == "" || strings.TrimSpace(c.EmailFrom) == "" {
			return nil, fmt.Errorf("SMTP_HOST and EMAIL_FROM are required in production because email OTP is enabled")
		}
	}
	return &Config{Env: c.Env, HTTPPort: c.HTTPPort, MongoURI: c.MongoURI, MongoDBName: c.MongoDBName, JWTSecret: c.JWTSecret, RedisURL: c.RedisURL, TrustedProxyCIDRs: c.TrustedProxyCIDRs, CORSAllowedOrigins: c.CORSAllowedOrigins, TLSCertFile: c.TLSCertFile, TLSKeyFile: c.TLSKeyFile, JWTAccessTTL: time.Duration(a) * time.Minute, JWTRefreshTTL: time.Duration(d) * 24 * time.Hour, OTPExpiry: time.Duration(o) * time.Minute, SMSWebhookURL: c.SMSWebhookURL, SMSAuthToken: c.SMSAuthToken, SMTPHost: c.SMTPHost, SMTPPort: c.SMTPPort, SMTPUser: c.SMTPUser, SMTPPassword: c.SMTPPassword, EmailFrom: c.EmailFrom, DevLogOTP: c.DevLogOTP}, nil
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
