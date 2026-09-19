package contextkeys

import "context"

type roleKey struct{}

func WithRole(ctx context.Context, role string) context.Context {
	return context.WithValue(ctx, roleKey{}, role)
}

func Role(ctx context.Context) (string, bool) {
	role, ok := ctx.Value(roleKey{}).(string)
	return role, ok
}
