package entity

import (
	"net"
	"strings"
	"time"
	"unicode"
	"unicode/utf8"
)

const (
	maxDeviceNameRunes       = 64
	maxDeviceModelRunes      = 64
	maxDeviceOSVersionRunes  = 64
	maxDeviceAppVersionRunes = 32
	maxDeviceFingerprintLen  = 128
)

// Device represents a single client device (Android install) that a user
// has authenticated from. It allows the system to track multi-device usage
// and to attribute sync records to the device that produced them.
type Device struct {
	// ID is the unique identifier of the device (client-generated or
	// server-issued at registration time).
	ID string

	// UserID is the owning user's ID.
	UserID string

	// Name is a human friendly label (e.g. "Pixel 8"). Display-only.
	Name string

	// Model is the hardware model reported by the client. Display-only.
	Model string

	// Platform is the normalised OS family ("android", "ios", ...). Display-only.
	Platform string

	// OSVersion is the operating-system version string. Display-only.
	OSVersion string

	// AppVersion is the client application version. Display-only.
	AppVersion string

	// LastIP is the client address seen at the last authentication. It is stored
	// in full for security auditing but must only ever be exposed masked.
	LastIP string

	// Fingerprint is an opaque, client-derived value that identifies one physical installation
	// across reinstalls and cleared data. It is a one-way hash computed on the device, never a raw
	// hardware identifier. It lets the server recognise that a new device id is an installation
	// that is already on the account, so the same device is never counted twice.
	Fingerprint string

	// LastSeenAt is the timestamp of the device's most recent activity
	// (e.g. last successful sync or authentication).
	LastSeenAt time.Time

	// CreatedAt is the timestamp the device was first registered.
	CreatedAt time.Time
}

// ApplyMetadata copies client-supplied descriptive values onto the device after
// sanitising them. Every value is untrusted: it is shown back to the user in the
// "Account devices" screen, so control characters and bidirectional-override
// characters (which could be used to spoof a different device name) are removed,
// lengths are capped, the platform is restricted to a known list and an address
// that does not parse as an IP is dropped. Zero-width non-joiner (U+200C) is
// preserved because Persian text depends on it.
func (d *Device) ApplyMetadata(name, model, platform, osVersion, appVersion, ip string) {
	d.Name = sanitizeDisplayText(name, maxDeviceNameRunes)
	d.Model = sanitizeDisplayText(model, maxDeviceModelRunes)
	d.Platform = normalizeDevicePlatform(platform)
	d.OSVersion = sanitizeDisplayText(osVersion, maxDeviceOSVersionRunes)
	d.AppVersion = sanitizeDisplayText(appVersion, maxDeviceAppVersionRunes)
	d.LastIP = normalizeDeviceIP(ip)
}

// DisplayName is the label shown to the user for this device: the name the client reported,
// falling back to the hardware model. It is empty for very old clients that sent neither.
func (d *Device) DisplayName() string {
	if d == nil {
		return ""
	}
	if d.Name != "" {
		return d.Name
	}
	return d.Model
}

// SetFingerprint stores the client's installation fingerprint. Only lowercase or uppercase hex
// up to maxDeviceFingerprintLen characters is accepted; anything else is dropped.
func (d *Device) SetFingerprint(value string) {
	d.Fingerprint = normalizeFingerprint(value)
}

func normalizeFingerprint(value string) string {
	value = strings.TrimSpace(value)
	if value == "" || len(value) > maxDeviceFingerprintLen {
		return ""
	}
	for _, r := range value {
		if !(r >= '0' && r <= '9' || r >= 'a' && r <= 'f' || r >= 'A' && r <= 'F') {
			return ""
		}
	}
	return strings.ToLower(value)
}

func sanitizeDisplayText(value string, maxRunes int) string {
	value = strings.ToValidUTF8(value, "")
	var b strings.Builder
	b.Grow(len(value))
	for _, r := range value {
		switch {
		case r == '\u200c' || r == '\u200d':
			b.WriteRune(r)
		case unicode.IsSpace(r):
			b.WriteRune(' ')
		case unicode.IsControl(r), unicode.Is(unicode.Cf, r), unicode.Is(unicode.Co, r):
			// dropped: control, format (incl. bidi overrides) and private-use characters
		default:
			b.WriteRune(r)
		}
	}
	out := strings.Join(strings.Fields(b.String()), " ")
	if utf8.RuneCountInString(out) > maxRunes {
		out = strings.TrimSpace(string([]rune(out)[:maxRunes]))
	}
	return out
}

func normalizeDevicePlatform(value string) string {
	switch p := strings.ToLower(strings.TrimSpace(value)); p {
	case "android", "ios", "web", "desktop":
		return p
	}
	return ""
}

func normalizeDeviceIP(value string) string {
	ip := net.ParseIP(strings.TrimSpace(value))
	if ip == nil {
		return ""
	}
	return ip.String()
}
