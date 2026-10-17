# Stage 1: Build
FROM golang:1.26-alpine AS builder
WORKDIR /app

# Go module proxy. Override when proxy.golang.org is blocked or unreliable on your network, e.g.
#   GOPROXY=https://goproxy.cn,direct   (set in deployments/.env, passed by docker-compose.dev.yml)
ARG GOPROXY=https://proxy.golang.org,direct
ENV GOPROXY=${GOPROXY}

# Install dependencies first for caching
COPY go.mod go.sum ./
RUN go mod download

# Copy source and build
COPY . .
# CGO_ENABLED=0 required for scratch/alpine execution
RUN CGO_ENABLED=0 GOOS=linux go build -trimpath -ldflags="-s -w" -o /app/bin/server ./cmd/api && \
    CGO_ENABLED=0 GOOS=linux go build -trimpath -ldflags="-s -w" -o /app/bin/purge-deleted ./cmd/purge-deleted && \
    if [ -d ./cmd/seed-admin ]; then \
        CGO_ENABLED=0 GOOS=linux go build -trimpath -ldflags="-s -w" -o /app/bin/seed-admin ./cmd/seed-admin; \
    else \
        echo "WARNING: ./cmd/seed-admin not found; building without the admin bootstrap CLI"; \
    fi

# Stage 2: Final minimal image
FROM alpine:3.19

WORKDIR /app

# Create a non-root user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Copies server, purge-deleted and (when present in the source tree) seed-admin:
#   docker compose exec api /app/seed-admin --identifier ...
#   docker compose exec api /app/purge-deleted --older-than-days 30   (run daily, see README)
COPY --from=builder /app/bin/ /app/

EXPOSE 8080

CMD ["/app/server"]