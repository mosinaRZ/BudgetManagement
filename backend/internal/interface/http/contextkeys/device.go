package contextkeys

import "context"

type deviceIDKey struct{}

// WithDeviceID stores the device id taken from a validated access token.
func WithDeviceID(ctx context.Context, deviceID string) context.Context {
	return context.WithValue(ctx, deviceIDKey{}, deviceID)
}

// DeviceID returns the calling device id, or ("", false) for legacy tokens without one.
func DeviceID(ctx context.Context) (string, bool) {
	deviceID, ok := ctx.Value(deviceIDKey{}).(string)
	return deviceID, ok && deviceID != ""
}
