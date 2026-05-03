package sqs

import (
	"context"
	"os"

	"github.com/aws/aws-sdk-go-v2/config"
	"github.com/aws/aws-sdk-go-v2/service/sqs"
	"github.com/chehsunliu/itx/itx-go/itx-contract/queue"
)

type MessageQueueFactory struct {
	client                  *sqs.Client
	controlStandardQueueURL string
	controlPremiumQueueURL  string
	computeStandardQueueURL string
	computePremiumQueueURL  string
}

func FromEnv() (*MessageQueueFactory, error) {
	cfg, err := config.LoadDefaultConfig(context.Background())
	if err != nil {
		return nil, err
	}

	endpoint := os.Getenv("ITX_SQS_LOCAL_ENDPOINT_URL")
	client := sqs.NewFromConfig(cfg, func(o *sqs.Options) {
		if endpoint != "" {
			o.BaseEndpoint = &endpoint
		}
	})

	return &MessageQueueFactory{
		client:                  client,
		controlStandardQueueURL: os.Getenv("ITX_SQS_CONTROL_STANDARD_QUEUE_URL"),
		controlPremiumQueueURL:  os.Getenv("ITX_SQS_CONTROL_PREMIUM_QUEUE_URL"),
		computeStandardQueueURL: os.Getenv("ITX_SQS_COMPUTE_STANDARD_QUEUE_URL"),
		computePremiumQueueURL:  os.Getenv("ITX_SQS_COMPUTE_PREMIUM_QUEUE_URL"),
	}, nil
}

func (f *MessageQueueFactory) CreateControlStandardQueue() queue.MessageQueue {
	return newMessageQueue(f.client, f.controlStandardQueueURL)
}

func (f *MessageQueueFactory) CreateControlPremiumQueue() queue.MessageQueue {
	return newMessageQueue(f.client, f.controlPremiumQueueURL)
}

func (f *MessageQueueFactory) CreateComputeStandardQueue() queue.MessageQueue {
	return newMessageQueue(f.client, f.computeStandardQueueURL)
}

func (f *MessageQueueFactory) CreateComputePremiumQueue() queue.MessageQueue {
	return newMessageQueue(f.client, f.computePremiumQueueURL)
}
