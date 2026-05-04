package sqs

import (
	"context"
	"os"
	"strconv"

	"github.com/aws/aws-sdk-go-v2/config"
	"github.com/aws/aws-sdk-go-v2/service/sqs"
	"github.com/chehsunliu/itx/itx-go/itx-contract/queue"
)

const defaultMaxConcurrency = 100

type MessageQueueFactory struct {
	client                  *sqs.Client
	maxConcurrency          int64
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

	maxConcurrency := int64(defaultMaxConcurrency)
	if raw := os.Getenv("ITX_SQS_MAX_CONCURRENCY"); raw != "" {
		if v, err := strconv.ParseInt(raw, 10, 64); err == nil && v > 0 {
			maxConcurrency = v
		}
	}

	return &MessageQueueFactory{
		client:                  client,
		maxConcurrency:          maxConcurrency,
		controlStandardQueueURL: os.Getenv("ITX_SQS_CONTROL_STANDARD_QUEUE_URL"),
		controlPremiumQueueURL:  os.Getenv("ITX_SQS_CONTROL_PREMIUM_QUEUE_URL"),
		computeStandardQueueURL: os.Getenv("ITX_SQS_COMPUTE_STANDARD_QUEUE_URL"),
		computePremiumQueueURL:  os.Getenv("ITX_SQS_COMPUTE_PREMIUM_QUEUE_URL"),
	}, nil
}

func (f *MessageQueueFactory) CreateControlStandardQueue() queue.MessageQueue {
	return newMessageQueue(f.client, f.controlStandardQueueURL, f.maxConcurrency)
}

func (f *MessageQueueFactory) CreateControlPremiumQueue() queue.MessageQueue {
	return newMessageQueue(f.client, f.controlPremiumQueueURL, f.maxConcurrency)
}

func (f *MessageQueueFactory) CreateComputeStandardQueue() queue.MessageQueue {
	return newMessageQueue(f.client, f.computeStandardQueueURL, f.maxConcurrency)
}

func (f *MessageQueueFactory) CreateComputePremiumQueue() queue.MessageQueue {
	return newMessageQueue(f.client, f.computePremiumQueueURL, f.maxConcurrency)
}
