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
}

type DeviceListResponse struct {
	Devices []DeviceResponse `json:"devices"`
}
