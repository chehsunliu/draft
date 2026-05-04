package compute

import (
	"context"
	"log/slog"
)

type Dispatcher struct {
	state WorkerState
}

func NewDispatcher(state WorkerState) *Dispatcher {
	return &Dispatcher{state: state}
}

func (d *Dispatcher) Handle(_ context.Context, body string) error {
	// No compute-plane message types yet — log and ack so the queue stays drained.
	slog.Info("compute message received (no handler yet)", "body", body)
	return nil
}
