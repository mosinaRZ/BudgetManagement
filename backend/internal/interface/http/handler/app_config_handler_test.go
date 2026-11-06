package handler

import (
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"testing"
)

func TestAppConfigHandlerReturnsPolicy(t *testing.T) {
	h := NewAppConfigHandler(AppConfig{
		MinVersion:    "1.1.2",
		LatestVersion: "1.2.0",
		UpdateURLs:    map[string]string{"play": "https://play.example/app", "myket": ""},
	})
	rec := httptest.NewRecorder()
	h.Get(rec, httptest.NewRequest(http.MethodGet, "/app-config", nil))

	if rec.Code != http.StatusOK {
		t.Fatalf("status = %d; want 200", rec.Code)
	}
	if cc := rec.Header().Get("Cache-Control"); cc == "" {
		t.Error("Cache-Control header is missing")
	}
	var body appConfigResponse
	if err := json.Unmarshal(rec.Body.Bytes(), &body); err != nil {
		t.Fatalf("invalid JSON: %v", err)
	}
	if body.MinVersion != "1.1.2" || body.LatestVersion != "1.2.0" {
		t.Errorf("versions = %q / %q; want 1.1.2 / 1.2.0", body.MinVersion, body.LatestVersion)
	}
	if body.UpdateURLs["play"] != "https://play.example/app" {
		t.Errorf("play URL = %q", body.UpdateURLs["play"])
	}
	if _, present := body.UpdateURLs["myket"]; present {
		t.Error("empty URLs must be left out of the response")
	}
}
