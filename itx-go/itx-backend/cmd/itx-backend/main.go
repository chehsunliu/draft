package main

import (
	"context"
	"errors"
	"fmt"
	"log"
	"net/http"
	"time"

	"github.com/chehsunliu/itx/itx-go/itx-backend/internal/app"
	"github.com/chehsunliu/itx/itx-go/itx-backend/internal/state"
	"github.com/spf13/cobra"
)

func main() {
	var host string
	var port int

	cmd := &cobra.Command{
		Use:   "itx-backend",
		Short: "ITX API server",
		RunE: func(_ *cobra.Command, _ []string) error {
			return run(host, port)
		},
	}

	cmd.Flags().StringVar(&host, "host", "127.0.0.1", "host to bind")
	cmd.Flags().IntVar(&port, "port", 8080, "port to bind")

	if err := cmd.Execute(); err != nil {
		log.Fatal(err)
	}
}

func run(host string, port int) error {
	addr := fmt.Sprintf("%s:%d", host, port)
	s, err := state.FromEnv()
	if err != nil {
		return fmt.Errorf("init state: %w", err)
	}
	router := app.NewRouter(s)

	srv := &http.Server{
		Addr:    addr,
		Handler: router,
	}

	ctx, cancel := app.ShutdownContext()
	defer cancel()

	errCh := make(chan error, 1)
	go func() {
		log.Printf("listening on %s", addr)
		if err := srv.ListenAndServe(); err != nil && !errors.Is(err, http.ErrServerClosed) {
			errCh <- err
		}
		close(errCh)
	}()

	select {
	case err := <-errCh:
		if err != nil {
			return err
		}
	case <-ctx.Done():
		log.Println("shutting down...")
	}

	shutdownCtx, shutdownCancel := context.WithTimeout(context.Background(), 10*time.Second)
	defer shutdownCancel()

	if err := srv.Shutdown(shutdownCtx); err != nil {
		return fmt.Errorf("shutdown: %w", err)
	}
	return nil
}
