package entity

// Role identifies the authorization tier assigned to an account.
// Roles are server-controlled; clients must never be allowed to choose them during registration.
type Role string

const (
	RoleUser    Role = "user"
	RolePro     Role = "pro"
	RolePremium Role = "premium"
	RoleAdmin   Role = "admin"
)

func (r Role) Valid() bool {
	switch r {
	case RoleUser, RolePro, RolePremium, RoleAdmin:
		return true
	default:
		return false
	}
}
