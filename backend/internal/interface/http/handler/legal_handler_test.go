package handler

import (
	"net/http"
	"net/http/httptest"
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestLegalPagesRedirectToTheWebsite(t *testing.T) {
	h := NewLegalHandler("")
	cases := []struct {
		name string
		fn   http.HandlerFunc
		slug string
	}{
		{"privacy", h.PrivacyPolicy, "privacy"},
		{"terms", h.Terms, "terms"},
		{"licenses", h.Licenses, "licenses"},
		{"deletion", h.AccountDeletion, "account-deletion"},
	}
	for _, tc := range cases {
		t.Run(tc.name, func(t *testing.T) {
			rec := httptest.NewRecorder()
			tc.fn(rec, httptest.NewRequest(http.MethodGet, "/", nil))
			assert.Equal(t, http.StatusMovedPermanently, rec.Code)
			assert.Equal(t, "https://cidna.ir/"+tc.slug+"/", rec.Header().Get("Location"))
			assert.Contains(t, rec.Header().Values("Vary"), "Accept-Language")
		})
	}
}

func TestLegalRedirectFollowsTheClientLanguage(t *testing.T) {
	h := NewLegalHandler("")
	cases := map[string]string{
		"":                        "https://cidna.ir/privacy/",
		"fa-IR,fa;q=0.9,en;q=0.5": "https://cidna.ir/privacy/",
		"en-US,en;q=0.9":          "https://cidna.ir/en/privacy/",
		"EN":                      "https://cidna.ir/en/privacy/",
		"de,en;q=0.8":             "https://cidna.ir/privacy/",
	}
	for header, want := range cases {
		t.Run(header, func(t *testing.T) {
			req := httptest.NewRequest(http.MethodGet, "/privacy-policy", nil)
			if header != "" {
				req.Header.Set("Accept-Language", header)
			}
			rec := httptest.NewRecorder()
			h.PrivacyPolicy(rec, req)
			assert.Equal(t, want, rec.Header().Get("Location"))
		})
	}
}
