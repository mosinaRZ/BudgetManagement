package main

import (
	"context"
	"encoding/base64"
	"flag"
	"fmt"
	"os"
	"strings"
	"time"

	"github.com/joho/godotenv"
	"github.com/mosinaRZ/finance-sync-backend/internal/domain/entity"
	"github.com/mosinaRZ/finance-sync-backend/internal/infrastructure/auth"
	"github.com/mosinaRZ/finance-sync-backend/internal/infrastructure/config"
	"github.com/mosinaRZ/finance-sync-backend/internal/infrastructure/mongodb"
	authusecase "github.com/mosinaRZ/finance-sync-backend/internal/usecase/auth"
)

func main() {
	_ = godotenv.Load()
	identifier := flag.String("identifier", "", "phone number or email")
	password := flag.String("password", "", "temporary password for a newly created admin")
	flag.Parse()
	if strings.TrimSpace(*identifier) == "" {
		fmt.Fprintln(os.Stderr, "--identifier is required")
		os.Exit(2)
	}

	cfg, err := config.Load()
	if err != nil {
		fail(err)
	}
	ctx, cancel := context.WithTimeout(context.Background(), 30*time.Second)
	defer cancel()
	client, err := mongodb.NewClient(ctx, cfg.MongoURI)
	if err != nil {
		fail(err)
	}
	defer mongodb.Disconnect(client)
	db := client.Database(cfg.MongoDBName)
	repo := mongodb.NewUserRepository(db)

	id := strings.TrimSpace(*identifier)
	var u *entity.User
	if strings.Contains(id, "@") {
		u, err = repo.FindByEmailHash(ctx, authusecase.IdentifierHashForCLI(strings.ToLower(id), cfg.JWTSecret))
	} else {
		phone, e := authusecase.NormalizePhoneForCLI(id)
		if e != nil {
			fail(fmt.Errorf("invalid phone number: %w", e))
		}
		u, err = repo.FindByPhoneHash(ctx, authusecase.IdentifierHashForCLI(phone, cfg.JWTSecret))
	}
	if err == nil && u != nil {
		if err = repo.UpdateRole(ctx, u.ID, entity.RoleAdmin); err != nil {
			fail(err)
		}
		fmt.Printf("promoted existing user %s to admin\n", u.ID)
		return
	}

	if *password == "" || len(*password) < 12 {
		fail(fmt.Errorf("user does not exist; --password (at least 12 characters) is required to create a new admin"))
	}
	salt, err := auth.GenerateSalt()
	if err != nil {
		fail(err)
	}
	passwordHash, err := auth.HashPassword(*password, salt)
	if err != nil {
		fail(err)
	}
	cryptoMaterial, err := auth.CreateClientKeyMaterial(*password)
	if err != nil {
		fail(err)
	}
	now := time.Now().UTC()
	u = &entity.User{
		Role: entity.RoleAdmin, PasswordHash: passwordHash, AuthSalt: authSalt(salt),
		KdfSalt:             cryptoMaterial.KdfSalt,
		PasswordKeyEnvelope: mustDecodeRawBase64(cryptoMaterial.PasswordEnvelope),
		PasswordKeyNonce:    mustDecodeRawBase64(cryptoMaterial.PasswordNonce),
		RecoveryKeyHash:     cryptoMaterial.RecoveryKeyHash,
		RecoveryKeyEnvelope: mustDecodeRawBase64(cryptoMaterial.RecoveryEnvelope),
		RecoveryKeyNonce:    mustDecodeRawBase64(cryptoMaterial.RecoveryNonce),
		CreatedAt:           now, UpdatedAt: now, Devices: []string{},
	}
	if strings.Contains(id, "@") {
		u.EmailHash = authusecase.IdentifierHashForCLI(strings.ToLower(id), cfg.JWTSecret)
		u.EmailVerified = true
	} else {
		phone, _ := authusecase.NormalizePhoneForCLI(id)
		u.PhoneHash = authusecase.IdentifierHashForCLI(phone, cfg.JWTSecret)
		u.PhoneVerified = true
	}
	if err := repo.Create(ctx, u); err != nil {
		fail(err)
	}
	fmt.Printf("created admin user %s; change the temporary bootstrap password immediately\n", u.ID)
	fmt.Printf("recovery key (store offline securely): %s\n", cryptoMaterial.RecoveryKey)
}

func authSalt(b []byte) string { return base64.RawStdEncoding.EncodeToString(b) }
func mustDecodeRawBase64(v string) []byte {
	b, err := base64.RawStdEncoding.DecodeString(v)
	if err != nil {
		fail(err)
	}
	return b
}
func fail(err error) { fmt.Fprintln(os.Stderr, "seed-admin:", err); os.Exit(1) }
