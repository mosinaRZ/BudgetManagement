package contextkeys

import "context"

type clientIPKey struct{}

// WithClientIP stores the resolved client address (already validated against trusted proxies).
func WithClientIP(ctx context.Context, ip string) context.Context {
	return context.WithValue(ctx, clientIPKey{}, ip)
}

// ClientIP returns the client address stored by the ClientIPContext middleware.
func ClientIP(ctx context.Context) (string, bool) {
	ip, ok := ctx.Value(clientIPKey{}).(string)
	return ip, ok
}
