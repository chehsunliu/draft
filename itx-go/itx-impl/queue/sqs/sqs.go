package sqs

import (
	"context"

	"github.com/aws/aws-sdk-go-v2/service/sqs"
	"github.com/chehsunliu/itx/itx-go/itx-contract/queue"
)

type messageQueue struct {
	client   *sqs.Client
	queueURL string
}

func newMessageQueue(client *sqs.Client, queueURL string) queue.MessageQueue {
	return &messageQueue{client: client, queueURL: queueURL}
}

func (q *messageQueue) Publish(ctx context.Context, body string) error {
	_, err := q.client.SendMessage(ctx, &sqs.SendMessageInput{
		QueueUrl:    &q.queueURL,
		MessageBody: &body,
	})
	return err
}
