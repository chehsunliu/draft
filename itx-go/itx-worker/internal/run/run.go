package run

import (
	"context"
	"log/slog"
	"os"
	"os/signal"
	"sync"
	"syscall"
	"time"

	"github.com/chehsunliu/itx/itx-go/itx-contract/queue"
)

// Run consumes from each queue concurrently, dispatching every message to handler. Returns when
// SIGTERM/SIGINT is received and all queue listeners finish (or the 30s shutdown grace expires).
func Run(queues []queue.MessageQueue, handler queue.MessageHandler) {
	ctx, cancel := context.WithCancel(context.Background())
	defer cancel()

	var wg sync.WaitGroup
	for _, q := range queues {
		wg.Add(1)
		go func(q queue.MessageQueue) {
			defer wg.Done()
			if err := q.Receive(ctx, handler); err != nil {
				slog.Error("queue listener errored", "error", err)
			}
		}(q)
	}

	// Block on signal.
	sig := make(chan os.Signal, 1)
	signal.Notify(sig, os.Interrupt, syscall.SIGTERM)
	<-sig
	slog.Info("shutdown signal received; cancelling listeners")
	cancel()

	done := make(chan struct{})
	go func() {
		wg.Wait()
		close(done)
	}()
	select {
	case <-done:
	case <-time.After(30 * time.Second):
		slog.Warn("some listeners did not finish within 30s")
	}
	slog.Info("worker shutdown complete")
}
