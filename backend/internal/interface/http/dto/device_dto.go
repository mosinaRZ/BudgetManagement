package dto

type DeviceResponse struct {
	ID         string `json:"id"`
	Name       string `json:"name,omitempty"`
	Model      string `json:"model,omitempty"`
	Platform   string `json:"platform,omitempty"`
	OSVersion  string `json:"os_version,omitempty"`
	AppVersion string `json:"app_version,omitempty"`
	// LastIP is always masked (last octet / interface identifier removed) before it leaves the server.
	LastIP     string `json:"last_ip,omitempty"`
	LastSeenAt int64  `json:"last_seen_at"`
	CreatedAt  int64  `json:"created_at"`
	// IsPrimary marks the first device registered on the account. Only that device can remove
	// other devices, and it can only be removed from itself.
	IsPrimary bool `json:"is_primary"`
}

type DeviceListResponse struct {
	Devices []DeviceResponse `json:"devices"`
}

// DeviceEventResponse is one entry of the account's device activity feed.
//
// Type is "signed_in" (a device that was not part of the account signed in) or "removed" (a
// device was removed from the account). ActorName is the device that caused the event; for a
// removal, the device that removed it. CreatedAtMs is when it happened, in Unix milliseconds.
type DeviceEventResponse struct {
	ID          string `json:"id"`
	Type        string `json:"type"`
	DeviceID    string `json:"device_id"`
	DeviceName  string `json:"device_name,omitempty"`
	Platform    string `json:"platform,omitempty"`
	ActorName   string `json:"actor_name,omitempty"`
	CreatedAtMs int64  `json:"created_at_ms"`
}

// DeviceEventListResponse is one page of the feed. CursorMs is the position to continue from
// on the next request (Unix milliseconds); it is 0 when nothing new was examined, in which
// case the client keeps the cursor it already has.
type DeviceEventListResponse struct {
	Events   []DeviceEventResponse `json:"events"`
	CursorMs int64                 `json:"cursor_ms"`
}
