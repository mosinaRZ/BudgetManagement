package config

import (
	"strings"
	"testing"
)

const (
	k1 = "jwt-secret-0123456789abcdef0123456789abcdef0123456789abcdef0123456789"
	k2 = "phone-key-0123456789abcdef0123456789abcdef0123456789abcdef0123456789"
	k3 = "otp-hmac-0123456789abcdef0123456789abcdef0123456789abcdef0123456789a"
	k4 = "decoy-key-0123456789abcdef0123456789abcdef0123456789abcdef0123456789"
)

func setDevEnv(t *testing.T) {
	t.Helper()
	for k, v := range map[string]string{
		"ENV": "dev", "HTTP_PORT": "8080", "MONGO_URI": "mongodb://localhost:27017", "MONGO_DB_NAME": "x",
		"JWT_ACCESS_TTL_MINUTES": "15", "JWT_REFRESH_TTL_DAYS": "30",
		"JWT_SECRET": k1, "PHONE_HASH_KEY": k2, "OTP_HMAC_KEY": k3, "LOGIN_DECOY_KEY": k4,
	} {
		t.Setenv(k, v)
	}
}

func TestLoadReadsIndependentKeys(t *testing.T) {
	setDevEnv(t)
	c, err := Load()
	if err != nil {
		t.Fatal(err)
	}
	if c.JWTSecret != k1 || c.PhoneHashKey != k2 || c.OTPCodeKey != k3 || c.DecoySaltKey != k4 {
		t.Fatalf("keys not loaded independently: %+v", c)
	}
}

func TestLoadRejectsMissingOrReusedKeys(t *testing.T) {
	for name, tc := range map[string]struct{ env, value, wantInErr string }{
		"missing phone key":  {"PHONE_HASH_KEY", "", "PHONE_HASH_KEY"},
		"missing otp key":    {"OTP_HMAC_KEY", "", "OTP_HMAC_KEY"},
		"missing decoy key":  {"LOGIN_DECOY_KEY", "", "LOGIN_DECOY_KEY"},
		"phone key is jwt":   {"PHONE_HASH_KEY", k1, "PHONE_HASH_KEY"},
		"otp key is phone":   {"OTP_HMAC_KEY", k2, "OTP_HMAC_KEY"},
		"decoy key is otp":   {"LOGIN_DECOY_KEY", k3, "LOGIN_DECOY_KEY"},
		"short phone key":    {"PHONE_HASH_KEY", "short", "PHONE_HASH_KEY"},
		"short otp key":      {"OTP_HMAC_KEY", strings.Repeat("a", 31), "OTP_HMAC_KEY"},
		"short decoy key":    {"LOGIN_DECOY_KEY", "tiny", "LOGIN_DECOY_KEY"},
		"decoy key equals j": {"LOGIN_DECOY_KEY", k1, "LOGIN_DECOY_KEY"},
	} {
		t.Run(name, func(t *testing.T) {
			setDevEnv(t)
			t.Setenv(tc.env, tc.value)
			_, err := Load()
			if err == nil {
				t.Fatal("expected an error")
			}
			if !strings.Contains(err.Error(), tc.wantInErr) {
				t.Fatalf("error %q does not mention %s", err, tc.wantInErr)
			}
		})
	}
}

func TestLoadMissingPhoneKeyMentionsMigration(t *testing.T) {
	setDevEnv(t)
	t.Setenv("PHONE_HASH_KEY", "")
	_, err := Load()
	if err == nil || !strings.Contains(err.Error(), "JWT_SECRET had before") {
		t.Fatalf("error should explain how to keep existing accounts, got %v", err)
	}
}

func TestLoadReadsKeysFromFiles(t *testing.T) {
	setDevEnv(t)
	dir := t.TempDir()
	write := func(name, v string) string {
		p := dir + "/" + name
		if err := osWriteFile(p, v); err != nil {
			t.Fatal(err)
		}
		return p
	}
	t.Setenv("PHONE_HASH_KEY", "")
	t.Setenv("PHONE_HASH_KEY_FILE", write("phone", k2+"\n"))
	c, err := Load()
	if err != nil {
		t.Fatal(err)
	}
	if c.PhoneHashKey != k2 {
		t.Fatalf("PHONE_HASH_KEY_FILE not honoured: %q", c.PhoneHashKey)
	}
}
