package dto

type DeviceResponse struct {
	ID         string `json:"id"`
	LastSeenAt int64  `json:"last_seen_at"`
	CreatedAt  int64  `json:"created_at"`
}

type DeviceListResponse struct {
	Devices []DeviceResponse `json:"devices"`
}
