// seed-admin creates the first administrator account. Run it once from the API container.
package main

import (
	"context"
	"crypto/hmac"
	"crypto/sha256"
	"encoding/base64"
	"flag"
	"fmt"
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
	identifier := flag.String("identifier", "", "administrator phone number")
	password := flag.String("password", "", "administrator password (minimum 12 characters)")
	flag.Parse()
	if err := run(*identifier, *password); err != nil {
		fmt.Fprintln(os.Stderr, "seed-admin:", err)
		os.Exit(1)
	}
}

func run(identifier, password string) error {
	identifier = strings.TrimSpace(identifier)
	if len(identifier) < 8 || len(identifier) > 16 || !strings.HasPrefix(identifier, "+") {
		return fmt.Errorf("--identifier must be an international phone number beginning with +")
	}
	for _, digit := range identifier[1:] {
		if digit < '0' || digit > '9' {
			return fmt.Errorf("--identifier must contain only digits after the + prefix")
		}
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
	// The administrator signs in through the same app protocol as everyone else: the server only
	// ever stores a hash of the password-derived authentication secret, plus the wrapped keys.
	material, err := infraauth.CreateClientKeyMaterial(password)
	if err != nil {
		return fmt.Errorf("derive key material: %w", err)
	}
	salt, err := infraauth.GenerateSalt()
	if err != nil {
		return fmt.Errorf("generate password salt: %w", err)
	}
	passwordHash, err := infraauth.HashPassword(material.AuthSecret, salt)
	if err != nil {
		return fmt.Errorf("hash authentication secret: %w", err)
	}
	passwordEnvelope, passwordNonce, recoveryHash, recoveryEnvelope, recoveryNonce, err := decodeMaterial(material)
	if err != nil {
		return err
	}
	now := time.Now().UTC()
	h := hmac.New(sha256.New, []byte(cfg.JWTSecret))
	_, _ = h.Write([]byte(strings.ToLower(identifier)))
	user := &entity.User{
		Role:          entity.RoleAdmin,
		PhoneNumber:   identifier,
		PhoneHash:     base64.RawURLEncoding.EncodeToString(h.Sum(nil)),
		PhoneVerified: true,
		PasswordHash:  passwordHash,
		AuthSalt:      base64.RawStdEncoding.EncodeToString(salt),
		KdfSalt:       material.KdfSalt,

		PasswordKeyEnvelope: passwordEnvelope,
		PasswordKeyNonce:    passwordNonce,
		RecoveryKeyHash:     base64.RawURLEncoding.EncodeToString(recoveryHash),
		RecoveryKeyEnvelope: recoveryEnvelope,
		RecoveryKeyNonce:    recoveryNonce,
		CreatedAt:           now, UpdatedAt: now,
		Devices: []string{},
	}
	if err := mongodb.NewUserRepository(db).Create(ctx, user); err != nil {
		return fmt.Errorf("create administrator: %w", err)
	}
	fmt.Printf("Administrator account created for %s (user id %s). Store the password securely and remove it from shell history.\n", identifier, user.ID)
	fmt.Printf("Recovery key (shown once, keep it offline): %s\n", material.RecoveryKey)
	return nil
}

func decodeMaterial(m infraauth.ClientKeyMaterial) (pe, pn, rh, re, rn []byte, err error) {
	dec := base64.StdEncoding.DecodeString
	if pe, err = dec(m.PasswordEnvelope); err != nil {
		return
	}
	if pn, err = dec(m.PasswordNonce); err != nil {
		return
	}
	if rh, err = dec(m.RecoveryKeyHash); err != nil {
		return
	}
	if re, err = dec(m.RecoveryEnvelope); err != nil {
		return
	}
	rn, err = dec(m.RecoveryNonce)
	return
}

// Keep the seed tool consistent with the current authentication identifier-hash format.
func authHash(identifier, secret string) string {
	return authusecase.IdentifierHashForCLI(identifier, secret)
}
