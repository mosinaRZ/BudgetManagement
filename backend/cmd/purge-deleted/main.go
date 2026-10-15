// Command purge-deleted permanently erases accounts that were soft-deleted (isDeleted=true) more
// than the restore window ago. Run it from cron/systemd once a day:
//
//	purge-deleted --older-than-days 30            # erase
//	purge-deleted --older-than-days 30 --dry-run  # only report
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
	defaultDays := int(authusecase.DefaultAccountRestoreWindow / (24 * time.Hour))
	days := flag.Int("older-than-days", defaultDays, "erase accounts deleted at least this many days ago (must not be shorter than the restore window)")
	dryRun := flag.Bool("dry-run", false, "report what would be erased without deleting anything")
	flag.Parse()
	if *days < defaultDays {
		fmt.Fprintf(os.Stderr, "--older-than-days must be at least %d (the account restore window)\n", defaultDays)
		os.Exit(2)
	}

	cfg, err := config.Load()
	if err != nil {
		fail(err)
	}
	ctx, cancel := context.WithTimeout(context.Background(), 10*time.Minute)
	defer cancel()
	client, err := mongodb.NewClient(ctx, cfg.MongoURI)
	if err != nil {
		fail(err)
	}
	defer mongodb.Disconnect(client)

	cutoff := time.Now().UTC().Add(-time.Duration(*days) * 24 * time.Hour)
	report, err := mongodb.PurgeDeletedUsers(ctx, client.Database(cfg.MongoDBName), cutoff, *dryRun)
	if err != nil {
		fail(err)
	}
	verb := "erased"
	if *dryRun {
		verb = "would erase"
	}
	fmt.Printf("%s %d account(s); sync_records=%d sync_operations=%d refresh_tokens=%d devices=%d device_events=%d recovery_sessions=%d\n",
		verb, report.Users, report.SyncRecords, report.SyncOperations, report.RefreshTokens, report.Devices, report.DeviceEvents, report.RecoverySessions)
}

func fail(err error) {
	fmt.Fprintln(os.Stderr, err)
	os.Exit(1)
}
