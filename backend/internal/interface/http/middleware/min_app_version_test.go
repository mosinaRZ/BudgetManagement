package middleware

import (
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"testing"
)

func serve(t *testing.T, minVersion, announced string) *httptest.ResponseRecorder {
	t.Helper()
	h := MinAppVersion(minVersion)(http.HandlerFunc(func(w http.ResponseWriter, _ *http.Request) {
		w.WriteHeader(http.StatusNoContent)
	}))
	req := httptest.NewRequest(http.MethodGet, "/api/v1/devices", nil)
	if announced != "" {
		req.Header.Set(AppVersionHeader, announced)
	}
	rec := httptest.NewRecorder()
	h.ServeHTTP(rec, req)
	return rec
}

func TestMinAppVersionRejectsOldBuilds(t *testing.T) {
	rec := serve(t, "1.1.2", "1.1.1")
	if rec.Code != http.StatusUpgradeRequired {
		t.Fatalf("status = %d; want 426", rec.Code)
	}
	var body struct {
		Error struct{ Code string } `json:"error"`
	}
	if err := json.Unmarshal(rec.Body.Bytes(), &body); err != nil || body.Error.Code != UpdateRequiredCode {
		t.Fatalf("body = %s; want error code %s", rec.Body.String(), UpdateRequiredCode)
	}
}

func TestMinAppVersionPassesSupportedAndUnannouncedClients(t *testing.T) {
	for _, announced := range []string{"1.1.2", "1.2.0", "2.0.0-debug", "", "not-a-version"} {
		if rec := serve(t, "1.1.2", announced); rec.Code != http.StatusNoContent {
			t.Errorf("announced %q: status = %d; want 204", announced, rec.Code)
		}
	}
}

func TestMinAppVersionDisabledWhenUnset(t *testing.T) {
	for _, minVersion := range []string{"", "garbage"} {
		if rec := serve(t, minVersion, "0.0.1"); rec.Code != http.StatusNoContent {
			t.Errorf("min %q: status = %d; want 204", minVersion, rec.Code)
		}
	}
}
