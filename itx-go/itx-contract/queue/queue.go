package queue

import "context"

// MessageHandler processes a single message body. Returning nil acks/deletes the message.
// Returning an error tells the queue to nack/reject so the broker can route to the DLQ.
type MessageHandler interface {
	Handle(ctx context.Context, body string) error
}

type MessageQueue interface {
	Publish(ctx context.Context, body string) error

	// Receive runs the consumer loop until ctx is cancelled. On cancel the impl stops pulling
	// new messages and waits for in-flight handlers to finish before returning.
	Receive(ctx context.Context, handler MessageHandler) error
}
