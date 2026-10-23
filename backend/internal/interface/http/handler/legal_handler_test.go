package handler

import (
	"net/http"
	"net/http/httptest"
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestLegalPagesRender(t *testing.T) {
	h := NewLegalHandler("")
	cases := map[string]http.HandlerFunc{
		"privacy":  h.PrivacyPolicy,
		"terms":    h.Terms,
		"licenses": h.Licenses,
		"deletion": h.AccountDeletion,
	}
	for name, fn := range cases {
		t.Run(name, func(t *testing.T) {
			rec := httptest.NewRecorder()
			fn(rec, httptest.NewRequest(http.MethodGet, "/", nil))
			assert.Equal(t, http.StatusOK, rec.Code)
			assert.Contains(t, rec.Header().Get("Content-Type"), "text/html")
		})
	}
}
