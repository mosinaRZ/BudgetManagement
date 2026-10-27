// Command rehash-phones recomputes every user's stored phone identifier hash with the current
// PHONE_HASH_KEY. Use it after rotating that key (for example after a suspected leak):
//
//  1. set the new PHONE_HASH_KEY and restart the API
//  2. rehash-phones --dry-run      # report only
//  3. rehash-phones                # convert
//
// The phone number itself is stored with the account, so no information is lost; only the
// lookup hash is replaced. It is idempotent and can be re-run safely.
package main

import (
	"context"
	"flag"
	"fmt"
	"os"
	"time"

	"github.com/joho/godotenv"
	"github.com/mosinaRZ/finance-sync-backend/internal/infrastructure/config"
	"github.com/mosinaRZ/finance-sync-backend/internal/infrastructure/mongodb"
	authusecase "github.com/mosinaRZ/finance-sync-backend/internal/usecase/auth"
)

func main() {
	_ = godotenv.Load()
	dryRun := flag.Bool("dry-run", false, "report what would change without writing anything")
	flag.Parse()

	cfg, err := config.Load()
	if err != nil {
		fail(err)
	}
	ctx, cancel := context.WithTimeout(context.Background(), 30*time.Minute)
	defer cancel()
	client, err := mongodb.NewClient(ctx, cfg.MongoURI)
	if err != nil {
		fail(err)
	}
	defer func() { _ = mongodb.Disconnect(client) }()

	report, err := mongodb.RehashPhones(ctx, client.Database(cfg.MongoDBName), func(phone string) string {
		return authusecase.IdentifierHashForCLI(phone, cfg.PhoneHashKey)
	}, *dryRun)
	if err != nil {
		fail(err)
	}
	verb := "updated"
	if *dryRun {
		verb = "would update"
	}
	fmt.Printf("%s %d account(s); already current=%d, without stored phone number=%d (scanned %d)\n",
		verb, report.Updated, report.Unchanged, report.NoPhone, report.Scanned)
	if report.NoPhone > 0 {
		fmt.Fprintln(os.Stderr, "warning: accounts without a stored phone number cannot be rehashed and will not be found after a key rotation")
	}
}

func fail(err error) {
	fmt.Fprintln(os.Stderr, err)
	os.Exit(1)
}
