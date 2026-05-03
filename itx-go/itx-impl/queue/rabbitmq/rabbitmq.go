package rabbitmq

import (
	"context"
	"sync"

	"github.com/chehsunliu/itx/itx-go/itx-contract/queue"
	amqp "github.com/rabbitmq/amqp091-go"
)

type messageQueue struct {
	conn      *amqp.Connection
	queueName string

	mu      sync.Mutex
	channel *amqp.Channel
}

func newMessageQueue(conn *amqp.Connection, queueName string) queue.MessageQueue {
	return &messageQueue{conn: conn, queueName: queueName}
}

func (q *messageQueue) publishChannel() (*amqp.Channel, error) {
	q.mu.Lock()
	defer q.mu.Unlock()
	if q.channel != nil && !q.channel.IsClosed() {
		return q.channel, nil
	}
	ch, err := q.conn.Channel()
	if err != nil {
		return nil, err
	}
	q.channel = ch
	return ch, nil
}

func (q *messageQueue) Publish(ctx context.Context, body string) error {
	ch, err := q.publishChannel()
	if err != nil {
		return err
	}
	return ch.PublishWithContext(ctx,
		"",
		q.queueName,
		false,
		false,
		amqp.Publishing{
			ContentType:  "application/json",
			DeliveryMode: amqp.Persistent,
			Body:         []byte(body),
		},
	)
}
