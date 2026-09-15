package main

import (
	"context"
	"errors"
	"fmt"
	"net/http"
	"os"
	"os/signal"
	"syscall"
	"time"

	"github.com/go-playground/validator/v10"

	"github.com/mosinaRZ/finance-sync-backend/internal/infrastructure/config"
	"github.com/mosinaRZ/finance-sync-backend/internal/infrastructure/logger"
	"github.com/mosinaRZ/finance-sync-backend/internal/infrastructure/mongodb"
	httpiface "github.com/mosinaRZ/finance-sync-backend/internal/interface/http"
	"github.com/mosinaRZ/finance-sync-backend/internal/interface/http/handler"
	authUsecase "github.com/mosinaRZ/finance-sync-backend/internal/usecase/auth"
	syncUsecase "github.com/mosinaRZ/finance-sync-backend/internal/usecase/sync"
)

func main() {
	if err := run(); err != nil {
		fmt.Fprintf(os.Stderr, "server error: %v\n", err)
		os.Exit(1)
	}
}

func run() error {
	cfg, err := config.Load()
	if err != nil {
		return fmt.Errorf("configuration error: %w", err)
	}

	log := logger.New(cfg.Env)
	log.Info("Starting finance sync service", "env", cfg.Env, "port", cfg.HTTPPort)

	startupCtx, startupCancel := context.WithTimeout(context.Background(), 30*time.Second)
	defer startupCancel()

	mongoClient, err := mongodb.NewClient(startupCtx, cfg.MongoURI)
	if err != nil {
		return fmt.Errorf("failed to initialize database: %w", err)
	}
	mongoConnected := true
	defer func() {
		if mongoConnected {
			if err := mongodb.Disconnect(mongoClient); err != nil {
				log.Error("Failed to disconnect mongo after startup failure", "error", err)
			}
		}
	}()

	db := mongoClient.Database(cfg.MongoDBName)
	if err := mongodb.EnsureIndexes(startupCtx, db); err != nil {
		return fmt.Errorf("failed to ensure database indexes: %w", err)
	}
	if err := mongodb.BackfillSyncRevisions(startupCtx, db); err != nil {
		return fmt.Errorf("failed to backfill sync revisions: %w", err)
	}

	validate := validator.New()
	syncRepo := mongodb.NewSyncRepository(mongoClient, db)
	syncUC := syncUsecase.NewSyncUsecase(syncRepo)
	syncHandler := handler.NewSyncHandler(syncUC, validate)

	userRepo := mongodb.NewUserRepository(db)
	refreshRepo := mongodb.NewRefreshTokenRepository(db)
	authUC := authUsecase.NewService(userRepo, refreshRepo, cfg.JWTSecret, cfg.JWTAccessTTL, cfg.JWTRefreshTTL)
	authHandler := handler.NewAuthHandler(authUC, validate)

	router := httpiface.NewRouter(httpiface.RouterDependencies{
		AuthHandler: authHandler,
		SyncHandler: syncHandler,
		JWTSecret:   cfg.JWTSecret,
	})

	srv := &http.Server{
		Addr:              fmt.Sprintf(":%s", cfg.HTTPPort),
		Handler:           router,
		ReadHeaderTimeout: 5 * time.Second,
		ReadTimeout:       15 * time.Second,
		WriteTimeout:      15 * time.Second,
		IdleTimeout:       60 * time.Second,
	}

	serverErrors := make(chan error, 1)
	go func() {
		log.Info("HTTP server is listening", "address", srv.Addr)
		serverErrors <- srv.ListenAndServe()
	}()

	shutdown := make(chan os.Signal, 1)
	signal.Notify(shutdown, os.Interrupt, syscall.SIGTERM)
	defer signal.Stop(shutdown)

	select {
	case err := <-serverErrors:
		if !errors.Is(err, http.ErrServerClosed) {
			return fmt.Errorf("http server error: %w", err)
		}
	case sig := <-shutdown:
		log.Info("Starting graceful shutdown", "signal", sig.String())
		shutdownCtx, cancel := context.WithTimeout(context.Background(), 10*time.Second)
		err := srv.Shutdown(shutdownCtx)
		cancel()
		if err != nil {
			log.Error("Could not gracefully shutdown server", "error", err)
			if closeErr := srv.Close(); closeErr != nil {
				log.Error("Could not close server", "error", closeErr)
			}
		}
	}

	if err := mongodb.Disconnect(mongoClient); err != nil {
		return fmt.Errorf("failed to disconnect mongo: %w", err)
	}
	mongoConnected = false
	log.Info("Server stopped")
	return nil
}
