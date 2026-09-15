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
	Env           string
	HTTPPort      string
	MongoURI      string
	MongoDBName   string
	JWTSecret     string
	JWTAccessTTL  time.Duration
	JWTRefreshTTL time.Duration
}

// Load reads the complete application configuration from the environment.
// .env is only a convenience for local development; environment variables are
// the single source of truth and there are no unsafe fallbacks.
func Load() (*Config, error) {
	_ = godotenv.Load()

	env := os.Getenv("ENV")
	if env == "" {
		env = "dev"
	}

	required := map[string]string{
		"HTTP_PORT":              os.Getenv("HTTP_PORT"),
		"MONGO_URI":              os.Getenv("MONGO_URI"),
		"MONGO_DB_NAME":          os.Getenv("MONGO_DB_NAME"),
		"JWT_SECRET":             os.Getenv("JWT_SECRET"),
		"JWT_ACCESS_TTL_MINUTES": os.Getenv("JWT_ACCESS_TTL_MINUTES"),
		"JWT_REFRESH_TTL_DAYS":   os.Getenv("JWT_REFRESH_TTL_DAYS"),
	}

	var missing []string
	for _, key := range []string{
		"HTTP_PORT",
		"MONGO_URI",
		"MONGO_DB_NAME",
		"JWT_SECRET",
		"JWT_ACCESS_TTL_MINUTES",
		"JWT_REFRESH_TTL_DAYS",
	} {
		if strings.TrimSpace(required[key]) == "" {
			missing = append(missing, key)
		}
	}
	if len(missing) > 0 {
		return nil, fmt.Errorf("configuration incomplete: missing required environment variables: %s", strings.Join(missing, ", "))
	}

	accessMinutes, err := positiveIntEnv("JWT_ACCESS_TTL_MINUTES", required["JWT_ACCESS_TTL_MINUTES"])
	if err != nil {
		return nil, err
	}
	refreshDays, err := positiveIntEnv("JWT_REFRESH_TTL_DAYS", required["JWT_REFRESH_TTL_DAYS"])
	if err != nil {
		return nil, err
	}

	port, err := strconv.Atoi(strings.TrimSpace(required["HTTP_PORT"]))
	if err != nil || port < 1 || port > 65535 {
		return nil, fmt.Errorf("configuration invalid: HTTP_PORT must be between 1 and 65535")
	}

	if len(required["JWT_SECRET"]) < 32 {
		return nil, fmt.Errorf("configuration invalid: JWT_SECRET must be at least 32 characters")
	}

	return &Config{
		Env:           env,
		HTTPPort:      required["HTTP_PORT"],
		MongoURI:      required["MONGO_URI"],
		MongoDBName:   required["MONGO_DB_NAME"],
		JWTSecret:     required["JWT_SECRET"],
		JWTAccessTTL:  time.Duration(accessMinutes) * time.Minute,
		JWTRefreshTTL: time.Duration(refreshDays) * 24 * time.Hour,
	}, nil
}

func positiveIntEnv(key, value string) (int, error) {
	parsed, err := strconv.Atoi(strings.TrimSpace(value))
	if err != nil || parsed <= 0 {
		return 0, fmt.Errorf("configuration invalid: %s must be a positive integer", key)
	}
	return parsed, nil
}
