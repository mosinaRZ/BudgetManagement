package dto

// Admin console payloads. All timestamps are Unix milliseconds. Nothing here ever carries
// credential material (hashes, salts, key envelopes); IP addresses are always masked.

type AdminDeviceResponse struct {
	ID           string `json:"id"`
	Name         string `json:"name,omitempty"`
	Model        string `json:"model,omitempty"`
	Platform     string `json:"platform,omitempty"`
	OSVersion    string `json:"os_version,omitempty"`
	AppVersion   string `json:"app_version,omitempty"`
	LastIP       string `json:"last_ip,omitempty"`
	LastSeenAtMs int64  `json:"last_seen_at_ms"`
	CreatedAtMs  int64  `json:"created_at_ms"`
	Online       bool   `json:"online"`
}

type AdminUserResponse struct {
	ID                  string   `json:"id"`
	PhoneNumber         string   `json:"phone_number,omitempty"`
	PhoneVerified       bool     `json:"phone_verified"`
	FirstName           string   `json:"first_name,omitempty"`
	LastName            string   `json:"last_name,omitempty"`
	Gender              string   `json:"gender,omitempty"`
	BirthDate           string   `json:"birth_date,omitempty"`
	Role                string   `json:"role"`
	CreatedAtMs         int64    `json:"created_at_ms"`
	UpdatedAtMs         int64    `json:"updated_at_ms"`
	IsDeleted           bool     `json:"is_deleted"`
	DeletedAtMs         int64    `json:"deleted_at_ms,omitempty"`
	LockedUntilMs       int64    `json:"locked_until_ms,omitempty"`
	FailedLoginAttempts int      `json:"failed_login_attempts"`
	Online              bool     `json:"online"`
	LastSeenAtMs        int64    `json:"last_seen_at_ms,omitempty"`
	DeviceCount         int      `json:"device_count"`
	Platforms           []string `json:"platforms"`
	AppVersion          string   `json:"app_version,omitempty"`
	SyncRecords         int64    `json:"sync_records"`
	SyncBytes           int64    `json:"sync_bytes"`
}

type AdminUserListResponse struct {
	Users           []AdminUserResponse `json:"users"`
	Total           int64               `json:"total"`
	Limit           int                 `json:"limit"`
	Offset          int                 `json:"offset"`
	OnlineWindowSec int64               `json:"online_window_sec"`
}

type AdminUserDetailResponse struct {
	User    AdminUserResponse     `json:"user"`
	Devices []AdminDeviceResponse `json:"devices"`
	Events  []AdminEventResponse  `json:"events"`
}

// UpdateAdminUserRequest uses pointers so an omitted field means "leave unchanged".
type UpdateAdminUserRequest struct {
	FirstName *string `json:"first_name"`
	LastName  *string `json:"last_name"`
	Role      *string `json:"role"`
}

type AdminEventResponse struct {
	ID          string `json:"id"`
	Type        string `json:"type"`
	Severity    string `json:"severity"`
	UserID      string `json:"user_id,omitempty"`
	UserLabel   string `json:"user_label,omitempty"`
	ActorUserID string `json:"actor_user_id,omitempty"`
	ActorLabel  string `json:"actor_label,omitempty"`
	DeviceName  string `json:"device_name,omitempty"`
	Platform    string `json:"platform,omitempty"`
	AppVersion  string `json:"app_version,omitempty"`
	IP          string `json:"ip,omitempty"`
	Detail      string `json:"detail,omitempty"`
	CreatedAtMs int64  `json:"created_at_ms"`
}

type AdminEventListResponse struct {
	Events []AdminEventResponse `json:"events"`
	Total  int64                `json:"total"`
	Limit  int                  `json:"limit"`
	Offset int                  `json:"offset"`
}

type AdminMeResponse struct {
	UserID string `json:"user_id"`
	Role   string `json:"role"`
}

type AdminBucket struct {
	Key   string `json:"key"`
	Count int64  `json:"count"`
}

type AdminLoginDay struct {
	Day     string `json:"day"`
	Success int64  `json:"success"`
	Failed  int64  `json:"failed"`
}

type AdminSeverityDay struct {
	Day     string `json:"day"`
	Info    int64  `json:"info"`
	Warning int64  `json:"warning"`
	Danger  int64  `json:"danger"`
}

type AdminSyncDay struct {
	Day     string `json:"day"`
	Records int64  `json:"records"`
	Bytes   int64  `json:"bytes"`
}

type AdminSignupDay struct {
	Day   string `json:"day"`
	Count int64  `json:"count"`
}

type AdminTopSyncUser struct {
	UserID  string `json:"user_id"`
	Label   string `json:"label,omitempty"`
	Records int64  `json:"records"`
	Bytes   int64  `json:"bytes"`
}

type AdminSuspiciousSource struct {
	IP       string `json:"ip"`
	Failed   int64  `json:"failed"`
	Accounts int64  `json:"accounts"`
}

type AdminKPIs struct {
	TotalUsers       int64 `json:"total_users"`
	ActiveUsers      int64 `json:"active_users"`
	DeletedUsers     int64 `json:"deleted_users"`
	LockedUsers      int64 `json:"locked_users"`
	OnlineNow        int64 `json:"online_now"`
	NewUsers         int64 `json:"new_users"`
	LoginsSuccess    int64 `json:"logins_success"`
	LoginsFailed     int64 `json:"logins_failed"`
	UniqueLoginUsers int64 `json:"unique_login_users"`
	DangerEvents     int64 `json:"danger_events"`
	LockedEvents     int64 `json:"locked_events"`
	SyncRecords      int64 `json:"sync_records"`
	SyncBytes        int64 `json:"sync_bytes"`
}

type AdminRisk struct {
	Score int    `json:"score"`
	Level string `json:"level"`
}

type AdminAnalyticsResponse struct {
	FromMs           int64                   `json:"from_ms"`
	ToMs             int64                   `json:"to_ms"`
	Days             int                     `json:"days"`
	KPIs             AdminKPIs               `json:"kpis"`
	Risk             AdminRisk               `json:"risk"`
	LoginsByDay      []AdminLoginDay         `json:"logins_by_day"`
	SeverityByDay    []AdminSeverityDay      `json:"severity_by_day"`
	SignupsByDay     []AdminSignupDay        `json:"signups_by_day"`
	Heatmap          [][]int64               `json:"heatmap"`
	EventTypes       []AdminBucket           `json:"event_types"`
	Roles            []AdminBucket           `json:"roles"`
	Platforms        []AdminBucket           `json:"platforms"`
	AppVersions      []AdminBucket           `json:"app_versions"`
	SyncByDay        []AdminSyncDay          `json:"sync_by_day"`
	SyncByEntity     []AdminBucket           `json:"sync_by_entity"`
	TopSyncUsers     []AdminTopSyncUser      `json:"top_sync_users"`
	Suspicious       []AdminSuspiciousSource `json:"suspicious_sources"`
	EventsTruncated  bool                    `json:"events_truncated"`
	EventsConsidered int                     `json:"events_considered"`
	OnlineWindowSec  int64                   `json:"online_window_sec"`
}
