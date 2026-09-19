package mongodb

import (
	"context"
	"fmt"
	"log/slog"
	"time"

	"go.mongodb.org/mongo-driver/mongo"
	"go.mongodb.org/mongo-driver/mongo/options"
	"go.mongodb.org/mongo-driver/mongo/readpref"
)

const (
	mongoConnectTimeout = 10 * time.Second
	mongoPingTimeout    = 10 * time.Second
	mongoMaxRetries     = 3
)

// NewClient creates and verifies a MongoDB client. A successful return means
// the primary is reachable and the client is ready for repository use.
func NewClient(ctx context.Context, uri string) (*mongo.Client, error) {
	clientOptions := options.Client().ApplyURI(uri)

	var lastErr error
	for attempt := 1; attempt <= mongoMaxRetries; attempt++ {
		client, err := mongo.Connect(ctx, clientOptions)
		if err != nil {
			lastErr = err
		} else {
			pingCtx, cancel := context.WithTimeout(ctx, mongoPingTimeout)
			err = client.Ping(pingCtx, readpref.Primary())
			cancel()
			if err == nil {
				slog.Info("successfully connected to MongoDB")
				return client, nil
			}
			lastErr = err

			disconnectCtx, disconnectCancel := context.WithTimeout(context.Background(), mongoConnectTimeout)
			_ = client.Disconnect(disconnectCtx)
			disconnectCancel()
		}

		slog.Warn("failed to connect to MongoDB, retrying", "attempt", attempt, "error", lastErr)
		if attempt < mongoMaxRetries {
			select {
			case <-ctx.Done():
				return nil, fmt.Errorf("mongodb connection canceled: %w", ctx.Err())
			case <-time.After(time.Duration(attempt*2) * time.Second):
			}
		}
	}

	return nil, fmt.Errorf("could not connect to mongodb after %d attempts: %w", mongoMaxRetries, lastErr)
}

// Disconnect closes the MongoDB connection pool using a bounded shutdown
// context. It is safe to call from the application's graceful shutdown path.
func Disconnect(client *mongo.Client) error {
	if client == nil {
		return nil
	}

	ctx, cancel := context.WithTimeout(context.Background(), mongoConnectTimeout)
	defer cancel()
	return client.Disconnect(ctx)
}
