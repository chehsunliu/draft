package queue

import "context"

type MessageQueue interface {
	Publish(ctx context.Context, body string) error
}
