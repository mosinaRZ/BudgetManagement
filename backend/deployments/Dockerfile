# Stage 1: Build
FROM golang:1.22-alpine AS builder

WORKDIR /app

# Install dependencies first for caching
COPY go.mod go.sum ./
RUN go mod download

# Copy source and build
COPY . .
# CGO_ENABLED=0 required for scratch/alpine execution
RUN CGO_ENABLED=0 GOOS=linux go build -o /app/bin/server ./cmd/api/main.go

# Stage 2: Final minimal image
FROM alpine:3.19

WORKDIR /app

# Create a non-root user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

COPY --from=builder /app/bin/server /app/server

EXPOSE 8080

CMD ["/app/server"]