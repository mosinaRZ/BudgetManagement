package config

import (
	"fmt"
	"strings"

	"github.com/mosinaRZ/finance-sync-backend/internal/pkg/appversion"
)

const (
	appPackageName = "ir.hamedan.budgetmanagement"

	// DefaultMinAppVersion is the oldest app version that is still allowed to run.
	DefaultMinAppVersion = "1.1.2"
	// DefaultLatestAppVersion is the newest published app version.
	DefaultLatestAppVersion = "1.2.0"
)

// AppUpdateConfig drives the in-app "update required" screen and the 426 check.
type AppUpdateConfig struct {
	MinVersion, LatestVersion string
	// UpdateURLs: "bazaar", "myket" and "default" (used when the store is unknown).
	// "myket" is left out of the map until APP_UPDATE_URL_MYKET is set.
	UpdateURLs map[string]string
}

// loadAppUpdate reads APP_MIN_VERSION, APP_LATEST_VERSION and APP_UPDATE_URL_{BAZAAR,MYKET,DEFAULT}.
// Every value is optional; the defaults match the current release. APP_UPDATE_URL_MYKET has no
// default: it stays empty (and is not sent to the app) until the Myket page exists.
func loadAppUpdate() (AppUpdateConfig, error) {
	minRaw := strings.TrimSpace(get("APP_MIN_VERSION", DefaultMinAppVersion))
	latestRaw := strings.TrimSpace(get("APP_LATEST_VERSION", DefaultLatestAppVersion))
	minV, ok := appversion.Parse(minRaw)
	if !ok {
		return AppUpdateConfig{}, fmt.Errorf("APP_MIN_VERSION %q is not a version such as 1.1.2", minRaw)
	}
	latestV, ok := appversion.Parse(latestRaw)
	if !ok {
		return AppUpdateConfig{}, fmt.Errorf("APP_LATEST_VERSION %q is not a version such as 1.2.0", latestRaw)
	}
	// A minimum above the published version would lock every user out with nothing to install.
	if latestV.Less(minV) {
		return AppUpdateConfig{}, fmt.Errorf("APP_LATEST_VERSION (%s) must not be lower than APP_MIN_VERSION (%s)", latestV, minV)
	}

	// optional: an empty value is allowed and the store is simply not offered.
	sources := []struct {
		key, env, fallback string
		optional           bool
	}{
		{"bazaar", "APP_UPDATE_URL_BAZAAR", "https://cafebazaar.ir/app/" + appPackageName + "?ref=share", false},
		{"myket", "APP_UPDATE_URL_MYKET", "", true},
		{"default", "APP_UPDATE_URL_DEFAULT", "https://cidna.ir", false},
	}
	urls := make(map[string]string, len(sources))
	for _, src := range sources {
		value := strings.TrimSpace(get(src.env, src.fallback))
		if value == "" && src.optional {
			continue
		}
		if !strings.HasPrefix(value, "https://") {
			return AppUpdateConfig{}, fmt.Errorf("%s must be an https:// URL", src.env)
		}
		urls[src.key] = value
	}
	return AppUpdateConfig{MinVersion: minRaw, LatestVersion: latestRaw, UpdateURLs: urls}, nil
}
