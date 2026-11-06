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
	// UpdateURLs: "play", "bazaar", "myket" and "default" (used when the store is unknown).
	UpdateURLs map[string]string
}

// loadAppUpdate reads APP_MIN_VERSION, APP_LATEST_VERSION and APP_UPDATE_URL_{PLAY,BAZAAR,MYKET,DEFAULT}.
// Every value is optional; the defaults match the current release.
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

	urls := map[string]string{
		"play":    get("APP_UPDATE_URL_PLAY", "https://play.google.com/store/apps/details?id="+appPackageName),
		"bazaar":  get("APP_UPDATE_URL_BAZAAR", "https://cafebazaar.ir/app/"+appPackageName),
		"myket":   get("APP_UPDATE_URL_MYKET", "https://myket.ir/app/"+appPackageName),
		"default": get("APP_UPDATE_URL_DEFAULT", "https://cidna.ir"),
	}
	for key, value := range urls {
		value = strings.TrimSpace(value)
		if !strings.HasPrefix(value, "https://") {
			return AppUpdateConfig{}, fmt.Errorf("APP_UPDATE_URL_%s must be an https:// URL", strings.ToUpper(key))
		}
		urls[key] = value
	}
	return AppUpdateConfig{MinVersion: minRaw, LatestVersion: latestRaw, UpdateURLs: urls}, nil
}
