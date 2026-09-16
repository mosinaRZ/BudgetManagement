package config

import (
	"fmt"
	"github.com/joho/godotenv"
	"os"
	"strconv"
	"strings"
	"time"
)

type Config struct {
	Env, HTTPPort, MongoURI, MongoDBName, JWTSecret                                    string
	JWTAccessTTL, JWTRefreshTTL, OTPExpiry                                             time.Duration
	SMSWebhookURL, SMSAuthToken, SMTPHost, SMTPPort, SMTPUser, SMTPPassword, EmailFrom string
	DevLogOTP                                                                          bool
}

func Load() (*Config, error) {
	_ = godotenv.Load()
	c := &Config{Env: get("ENV", "dev"), HTTPPort: os.Getenv("HTTP_PORT"), MongoURI: os.Getenv("MONGO_URI"), MongoDBName: os.Getenv("MONGO_DB_NAME"), JWTSecret: os.Getenv("JWT_SECRET"), SMSWebhookURL: os.Getenv("SMS_WEBHOOK_URL"), SMSAuthToken: os.Getenv("SMS_AUTH_TOKEN"), SMTPHost: os.Getenv("SMTP_HOST"), SMTPPort: get("SMTP_PORT", "587"), SMTPUser: os.Getenv("SMTP_USER"), SMTPPassword: os.Getenv("SMTP_PASSWORD"), EmailFrom: os.Getenv("EMAIL_FROM"), DevLogOTP: strings.EqualFold(get("DEV_LOG_OTP", "false"), "true")}
	for _, k := range []string{"HTTP_PORT", "MONGO_URI", "MONGO_DB_NAME", "JWT_SECRET", "JWT_ACCESS_TTL_MINUTES", "JWT_REFRESH_TTL_DAYS"} {
		if strings.TrimSpace(os.Getenv(k)) == "" {
			return nil, fmt.Errorf("configuration incomplete: missing %s", k)
		}
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
	if c.Env == "prod" && c.DevLogOTP {
		return nil, fmt.Errorf("DEV_LOG_OTP cannot be enabled in prod")
	}
	return &Config{Env: c.Env, HTTPPort: c.HTTPPort, MongoURI: c.MongoURI, MongoDBName: c.MongoDBName, JWTSecret: c.JWTSecret, JWTAccessTTL: time.Duration(a) * time.Minute, JWTRefreshTTL: time.Duration(d) * 24 * time.Hour, OTPExpiry: time.Duration(o) * time.Minute, SMSWebhookURL: c.SMSWebhookURL, SMSAuthToken: c.SMSAuthToken, SMTPHost: c.SMTPHost, SMTPPort: c.SMTPPort, SMTPUser: c.SMTPUser, SMTPPassword: c.SMTPPassword, EmailFrom: c.EmailFrom, DevLogOTP: c.DevLogOTP}, nil
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
