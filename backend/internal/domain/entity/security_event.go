package entity

import "time"

// SecurityEventType names one thing worth remembering about an account for the admin console:
// sign-ins (good and bad), credential changes, device removals and every administrative action.
type SecurityEventType string

const (
	SecLoginSuccess      SecurityEventType = "login_success"
	SecLoginFailed       SecurityEventType = "login_failed"
	SecAccountLocked     SecurityEventType = "account_locked"
	SecRegistered        SecurityEventType = "registered"
	SecPasswordChanged   SecurityEventType = "password_changed"
	SecPasswordReset     SecurityEventType = "password_reset"
	SecAccountDeleted    SecurityEventType = "account_deleted"
	SecAccountRestored   SecurityEventType = "account_restored"
	SecDeviceRemoved     SecurityEventType = "device_removed"
	SecAdminUserEdited   SecurityEventType = "admin_user_edited"
	SecAdminRoleChanged  SecurityEventType = "admin_role_changed"
	SecAdminUserDeleted  SecurityEventType = "admin_user_deleted"
	SecAdminUserRestored SecurityEventType = "admin_user_restored"
	SecAdminUnlocked     SecurityEventType = "admin_user_unlocked"
	SecAdminRevoked      SecurityEventType = "admin_sessions_revoked"
	SecAdminDenied       SecurityEventType = "admin_access_denied"
)

// Severity buckets used by the analytics filters and the colour coding of the console.
const (
	SeverityInfo    = "info"
	SeverityWarning = "warning"
	SeverityDanger  = "danger"
)

// SeverityOf classifies an event. Dangerous means "an administrator should look at this":
// lockouts (likely guessing), unauthorised admin attempts, credential resets and any
// destructive or privilege-changing admin action.
func SeverityOf(t SecurityEventType) string {
	switch t {
	case SecAccountLocked, SecAdminDenied, SecPasswordReset, SecAccountDeleted,
		SecAdminUserDeleted, SecAdminRoleChanged:
		return SeverityDanger
	case SecLoginFailed, SecPasswordChanged, SecDeviceRemoved, SecAdminRevoked,
		SecAdminUnlocked, SecAdminUserEdited:
		return SeverityWarning
	default:
		return SeverityInfo
	}
}

// AllSecurityEventTypes lists every known type (used to validate filters).
var AllSecurityEventTypes = []SecurityEventType{
	SecLoginSuccess, SecLoginFailed, SecAccountLocked, SecRegistered, SecPasswordChanged,
	SecPasswordReset, SecAccountDeleted, SecAccountRestored, SecDeviceRemoved,
	SecAdminUserEdited, SecAdminRoleChanged, SecAdminUserDeleted, SecAdminUserRestored,
	SecAdminUnlocked, SecAdminRevoked, SecAdminDenied,
}

// ValidSecurityEventType reports whether t is a known type.
func ValidSecurityEventType(t string) bool {
	for _, k := range AllSecurityEventTypes {
		if string(k) == t {
			return true
		}
	}
	return false
}

// SecurityEvent is one append-only record. UserID is the account the event is about (empty for
// a failed sign-in with an unknown identifier); ActorUserID is set when an administrator did it.
type SecurityEvent struct {
	ID          string
	Type        SecurityEventType
	UserID      string
	ActorUserID string
	DeviceID    string
	DeviceName  string
	Platform    string
	AppVersion  string
	IP          string // stored in full, only ever exposed masked
	Detail      string // short machine-readable reason, never secrets or PII
	CreatedAt   time.Time
}
