package handler

import (
	"net/http"

	"github.com/mosinaRZ/finance-sync-backend/internal/pkg/response"
)

// AppConfig is what the Android app needs to decide whether it must be updated.
type AppConfig struct {
	// MinVersion is the oldest app version that may still be used. Older builds are blocked
	// behind a non-dismissible update screen.
	MinVersion string
	// LatestVersion is the newest published version (shown on the update screen).
	LatestVersion string
	// UpdateURLs maps a store key ("play", "bazaar", "myket", "default") to the page where the
	// update can be installed.
	UpdateURLs map[string]string
}

type appConfigResponse struct {
	MinVersion    string            `json:"minVersion"`
	LatestVersion string            `json:"latestVersion"`
	UpdateURLs    map[string]string `json:"updateUrls"`
}

// AppConfigHandler serves the public, unauthenticated app configuration (GET /app-config).
// The app must be able to read it before anyone has signed in.
type AppConfigHandler struct {
	payload appConfigResponse
}

func NewAppConfigHandler(cfg AppConfig) *AppConfigHandler {
	urls := make(map[string]string, len(cfg.UpdateURLs))
	for key, value := range cfg.UpdateURLs {
		if value != "" {
			urls[key] = value
		}
	}
	return &AppConfigHandler{payload: appConfigResponse{
		MinVersion:    cfg.MinVersion,
		LatestVersion: cfg.LatestVersion,
		UpdateURLs:    urls,
	}}
}

func (h *AppConfigHandler) Get(w http.ResponseWriter, _ *http.Request) {
	// A short cache keeps a burst of app launches (many users behind one carrier NAT) cheap
	// while a changed minimum version still reaches everyone within a minute.
	w.Header().Set("Cache-Control", "public, max-age=60")
	response.JSON(w, http.StatusOK, h.payload)
}
