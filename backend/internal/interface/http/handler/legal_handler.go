package handler

import (
	"net/http"
	"strings"
)

// LegalHandler keeps the old public legal addresses alive.
//
// The privacy policy, terms of service, open-source notices and account-deletion instructions now
// live on the official website (legalSiteBase). Every legacy address answers with a permanent
// redirect to the matching website page, so links shipped in older app versions and entered in
// store listings keep working, while there is only one copy of each text to maintain.
type LegalHandler struct{}

// legalSiteBase is the official website that hosts the legal pages.
const legalSiteBase = "https://cidna.ir"

func NewLegalHandler(_ string) *LegalHandler { return &LegalHandler{} }

func (h *LegalHandler) PrivacyPolicy(w http.ResponseWriter, r *http.Request) {
	redirectToWebsite(w, r, "privacy")
}

func (h *LegalHandler) Terms(w http.ResponseWriter, r *http.Request) {
	redirectToWebsite(w, r, "terms")
}

func (h *LegalHandler) Licenses(w http.ResponseWriter, r *http.Request) {
	redirectToWebsite(w, r, "licenses")
}

func (h *LegalHandler) AccountDeletion(w http.ResponseWriter, r *http.Request) {
	redirectToWebsite(w, r, "account-deletion")
}

func redirectToWebsite(w http.ResponseWriter, r *http.Request, slug string) {
	w.Header().Add("Vary", "Accept-Language")
	http.Redirect(w, r, legalURL(slug, prefersEnglish(r.Header.Get("Accept-Language"))), http.StatusMovedPermanently)
}

// legalURL returns the website address of a legal page: Persian at /slug/, English at /en/slug/.
func legalURL(slug string, english bool) string {
	if english {
		return legalSiteBase + "/en/" + slug + "/"
	}
	return legalSiteBase + "/" + slug + "/"
}

// prefersEnglish reports whether English is the first language the client asks for.
// Anything else (including no header) gets the Persian page, the app's primary language.
func prefersEnglish(acceptLanguage string) bool {
	first := strings.TrimSpace(strings.SplitN(acceptLanguage, ",", 2)[0])
	return strings.HasPrefix(strings.ToLower(first), "en")
}
