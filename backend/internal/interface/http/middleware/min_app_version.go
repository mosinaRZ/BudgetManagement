package middleware

import (
	"net/http"

	"github.com/mosinaRZ/finance-sync-backend/internal/pkg/appversion"
	"github.com/mosinaRZ/finance-sync-backend/internal/pkg/response"
)

// AppVersionHeader is sent by the Android app with every request (its versionName).
const AppVersionHeader = "X-App-Version"

// UpdateRequiredCode is the error code the app recognises as "this build is too old".
const UpdateRequiredCode = "UPDATE_REQUIRED"

// MinAppVersion answers 426 Upgrade Required to app builds older than minVersion.
//
// It is the server-side backstop of the in-app update screen: it also stops background work
// of an old build and a client that skipped the screen. Only requests that announce a version
// can be judged. A request without the header (builds that predate it, browsers, tools) is
// passed through, because it cannot be told apart from a legitimate client. An empty or
// unparsable minVersion disables the check.
func MinAppVersion(minVersion string) func(http.Handler) http.Handler {
	floor, enabled := appversion.Parse(minVersion)
	return func(next http.Handler) http.Handler {
		if !enabled {
			return next
		}
		return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
			if v, announced := appversion.Parse(r.Header.Get(AppVersionHeader)); announced && v.Less(floor) {
				response.JSON(w, http.StatusUpgradeRequired, response.ErrorResponse{Error: response.ErrorDetail{
					Code:    UpdateRequiredCode,
					Message: "This app version is no longer supported. Please update the app.",
				}})
				return
			}
			next.ServeHTTP(w, r)
		})
	}
}
