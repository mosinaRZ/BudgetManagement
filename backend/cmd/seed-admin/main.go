// seed-admin creates the first administrator account. Run it once from the API container.
package main

import (
	"context"
	"encoding/base64"
	"flag"
	"fmt"
	"net/mail"
	"os"
	"strings"
	"time"

	"github.com/mosinaRZ/finance-sync-backend/internal/domain/entity"
	infraauth "github.com/mosinaRZ/finance-sync-backend/internal/infrastructure/auth"
	"github.com/mosinaRZ/finance-sync-backend/internal/infrastructure/config"
	"github.com/mosinaRZ/finance-sync-backend/internal/infrastructure/mongodb"
	authusecase "github.com/mosinaRZ/finance-sync-backend/internal/usecase/auth"
	"go.mongodb.org/mongo-driver/bson"
)

func main() {
	identifier := flag.String("identifier", "", "administrator email address")
	password := flag.String("password", "", "administrator password (minimum 12 characters)")
	flag.Parse()
	if err := run(*identifier, *password); err != nil {
		fmt.Fprintln(os.Stderr, "seed-admin:", err)
		os.Exit(1)
	}
}

func run(identifier, password string) error {
	identifier = strings.ToLower(strings.TrimSpace(identifier))
	parsed, err := mail.ParseAddress(identifier)
	if err != nil || parsed.Address != identifier {
		return fmt.Errorf("--identifier must be a valid email address")
	}
	if len(password) < 12 || len(password) > 256 {
		return fmt.Errorf("--password must be between 12 and 256 characters")
	}
	cfg, err := config.Load()
	if err != nil {
		return fmt.Errorf("load configuration: %w", err)
	}
	ctx, cancel := context.WithTimeout(context.Background(), 20*time.Second)
	defer cancel()
	client, err := mongodb.NewClient(ctx, cfg.MongoURI)
	if err != nil {
		return fmt.Errorf("connect to database: %w", err)
	}
	defer func() { _ = mongodb.Disconnect(client) }()
	db := client.Database(cfg.MongoDBName)
	if err := mongodb.EnsureIndexes(ctx, db); err != nil {
		return fmt.Errorf("ensure indexes: %w", err)
	}
	count, err := db.Collection("users").CountDocuments(ctx, bson.M{"role": entity.RoleAdmin})
	if err != nil {
		return fmt.Errorf("check existing administrators: %w", err)
	}
	if count > 0 {
		return fmt.Errorf("an administrator already exists; refusing to create another bootstrap administrator")
	}
	salt, err := infraauth.GenerateSalt()
	if err != nil {
		return fmt.Errorf("generate password salt: %w", err)
	}
	passwordHash, err := infraauth.HashPassword(password, salt)
	if err != nil {
		return fmt.Errorf("hash password: %w", err)
	}
	now := time.Now().UTC()
	user := &entity.User{
		Role:          entity.RoleAdmin,
		Email:         identifier,
		EmailHash:     authHash(identifier, cfg.JWTSecret),
		EmailVerified: true,
		PasswordHash:  passwordHash,
		AuthSalt:      base64.RawStdEncoding.EncodeToString(salt),
		CreatedAt:     now, UpdatedAt: now,
		Devices: []string{},
	}
	if err := mongodb.NewUserRepository(db).Create(ctx, user); err != nil {
		return fmt.Errorf("create administrator: %w", err)
	}
	fmt.Printf("Administrator account created for %s (user id %s). Store the password securely and remove it from shell history.\n", identifier, user.ID)
	return nil
}

// Keep the seed tool consistent with the current authentication identifier-hash format.
func authHash(identifier, secret string) string {
	return authusecase.IdentifierHashForCLI(identifier, secret)
}
