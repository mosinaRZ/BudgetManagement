package config

import "testing"

func TestLoadAppUpdateDefaults(t *testing.T) {
	for _, k := range []string{"APP_MIN_VERSION", "APP_LATEST_VERSION", "APP_UPDATE_URL_PLAY", "APP_UPDATE_URL_BAZAAR", "APP_UPDATE_URL_MYKET", "APP_UPDATE_URL_DEFAULT"} {
		t.Setenv(k, "")
	}
	got, err := loadAppUpdate()
	if err != nil {
		t.Fatalf("loadAppUpdate: %v", err)
	}
	if got.MinVersion != DefaultMinAppVersion || got.LatestVersion != DefaultLatestAppVersion {
		t.Errorf("versions = %s / %s", got.MinVersion, got.LatestVersion)
	}
	for _, key := range []string{"play", "bazaar", "myket", "default"} {
		if got.UpdateURLs[key] == "" {
			t.Errorf("missing default URL for %s", key)
		}
	}
}

func TestLoadAppUpdateRejectsBadValues(t *testing.T) {
	cases := map[string]map[string]string{
		"unparsable minimum":       {"APP_MIN_VERSION": "soon"},
		"unparsable latest":        {"APP_LATEST_VERSION": "x"},
		"minimum above latest":     {"APP_MIN_VERSION": "1.3.0", "APP_LATEST_VERSION": "1.2.0"},
		"non-https update address": {"APP_UPDATE_URL_PLAY": "http://example.com"},
	}
	for name, env := range cases {
		t.Run(name, func(t *testing.T) {
			for _, k := range []string{"APP_MIN_VERSION", "APP_LATEST_VERSION", "APP_UPDATE_URL_PLAY"} {
				t.Setenv(k, "")
			}
			for k, v := range env {
				t.Setenv(k, v)
			}
			if _, err := loadAppUpdate(); err == nil {
				t.Fatal("expected an error")
			}
		})
	}
}
