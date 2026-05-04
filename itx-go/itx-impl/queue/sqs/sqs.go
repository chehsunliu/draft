package sqs

import (
	"context"
	"errors"
	"log/slog"
	"sync"

	"github.com/aws/aws-sdk-go-v2/aws"
	"github.com/aws/aws-sdk-go-v2/service/sqs"
	"github.com/chehsunliu/itx/itx-go/itx-contract/queue"
	"golang.org/x/sync/semaphore"
)

type messageQueue struct {
	client         *sqs.Client
	queueURL       string
	maxConcurrency int64
	sem            *semaphore.Weighted
}

func newMessageQueue(client *sqs.Client, queueURL string, maxConcurrency int64) queue.MessageQueue {
	return &messageQueue{
		client:         client,
		queueURL:       queueURL,
		maxConcurrency: maxConcurrency,
		sem:            semaphore.NewWeighted(maxConcurrency),
	}
}

func (q *messageQueue) Publish(ctx context.Context, body string) error {
	_, err := q.client.SendMessage(ctx, &sqs.SendMessageInput{
		QueueUrl:    &q.queueURL,
		MessageBody: &body,
	})
	return err
}

func (q *messageQueue) Receive(ctx context.Context, handler queue.MessageHandler) error {
	batch := q.maxConcurrency
	if batch > 10 {
		batch = 10
	}
	if batch < 1 {
		batch = 1
	}
	var wg sync.WaitGroup

	for {
		if ctx.Err() != nil {
			break
		}
		resp, err := q.client.ReceiveMessage(ctx, &sqs.ReceiveMessageInput{
			QueueUrl:            &q.queueURL,
			MaxNumberOfMessages: int32(batch),
			WaitTimeSeconds:     20,
		})
		if err != nil {
			if errors.Is(err, context.Canceled) {
				break
			}
			wg.Wait()
			return err
		}

		for _, msg := range resp.Messages {
			if err := q.sem.Acquire(ctx, 1); err != nil {
				break
			}
			body := aws.ToString(msg.Body)
			receipt := aws.ToString(msg.ReceiptHandle)
			wg.Add(1)
			go func() {
				defer wg.Done()
				defer q.sem.Release(1)
				if err := handler.Handle(ctx, body); err != nil {
					slog.Warn("handler failed; leaving message for retry/DLQ", "error", err)
					return
				}
				// Use background context — we want delete to land even if shutdown is in flight.
				if _, err := q.client.DeleteMessage(context.Background(), &sqs.DeleteMessageInput{
					QueueUrl:      &q.queueURL,
					ReceiptHandle: &receipt,
				}); err != nil {
					slog.Error("failed to delete message after success", "error", err)
				}
			}()
		}
	}

	slog.Info("draining in-flight handlers", "queue", q.queueURL)
	wg.Wait()
	return nil
}
