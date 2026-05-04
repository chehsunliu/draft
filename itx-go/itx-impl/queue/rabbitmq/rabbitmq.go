package rabbitmq

import (
	"context"
	"fmt"
	"log/slog"
	"math"
	"sync"

	"github.com/chehsunliu/itx/itx-go/itx-contract/queue"
	"github.com/google/uuid"
	amqp "github.com/rabbitmq/amqp091-go"
	"golang.org/x/sync/semaphore"
)

type messageQueue struct {
	conn           *amqp.Connection
	queueName      string
	maxConcurrency int64
	sem            *semaphore.Weighted

	mu          sync.Mutex
	publishChan *amqp.Channel
	consumeChan *amqp.Channel
	consumerTag string
}

func newMessageQueue(conn *amqp.Connection, queueName string, maxConcurrency int64) queue.MessageQueue {
	return &messageQueue{
		conn:           conn,
		queueName:      queueName,
		maxConcurrency: maxConcurrency,
		sem:            semaphore.NewWeighted(maxConcurrency),
		consumerTag:    fmt.Sprintf("itx-%s", uuid.NewString()),
	}
}

func (q *messageQueue) publishChannel() (*amqp.Channel, error) {
	q.mu.Lock()
	defer q.mu.Unlock()
	if q.publishChan != nil && !q.publishChan.IsClosed() {
		return q.publishChan, nil
	}
	ch, err := q.conn.Channel()
	if err != nil {
		return nil, err
	}
	q.publishChan = ch
	return ch, nil
}

func (q *messageQueue) consumeChannel() (*amqp.Channel, error) {
	q.mu.Lock()
	defer q.mu.Unlock()
	if q.consumeChan != nil && !q.consumeChan.IsClosed() {
		return q.consumeChan, nil
	}
	ch, err := q.conn.Channel()
	if err != nil {
		return nil, err
	}
	prefetch := q.maxConcurrency
	if prefetch > math.MaxUint16 {
		prefetch = math.MaxUint16
	}
	if err := ch.Qos(int(prefetch), 0, false); err != nil {
		return nil, err
	}
	q.consumeChan = ch
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

func (q *messageQueue) Receive(ctx context.Context, handler queue.MessageHandler) error {
	ch, err := q.consumeChannel()
	if err != nil {
		return err
	}
	deliveries, err := ch.ConsumeWithContext(ctx, q.queueName, q.consumerTag, false, false, false, false, nil)
	if err != nil {
		return err
	}

	var wg sync.WaitGroup

	for {
		select {
		case <-ctx.Done():
			slog.Info("cancellation received; stopping receive", "queue", q.queueName)
			wg.Wait()
			return nil
		case delivery, ok := <-deliveries:
			if !ok {
				wg.Wait()
				return nil
			}
			if err := q.sem.Acquire(ctx, 1); err != nil {
				wg.Wait()
				return nil
			}
			wg.Add(1)
			go func(delivery amqp.Delivery) {
				defer wg.Done()
				defer q.sem.Release(1)
				if err := handler.Handle(ctx, string(delivery.Body)); err != nil {
					slog.Warn("handler failed; rejecting to DLQ", "error", err)
					if err := delivery.Reject(false); err != nil {
						slog.Error("failed to reject message", "error", err)
					}
					return
				}
				if err := delivery.Ack(false); err != nil {
					slog.Error("failed to ack message", "error", err)
				}
			}(delivery)
		}
	}
}
