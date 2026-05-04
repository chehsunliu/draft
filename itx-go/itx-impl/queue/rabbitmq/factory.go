package rabbitmq

import (
	"fmt"
	"os"
	"strconv"

	"github.com/chehsunliu/itx/itx-go/itx-contract/queue"
	amqp "github.com/rabbitmq/amqp091-go"
)

const defaultMaxConcurrency = 100

type MessageQueueFactory struct {
	conn                     *amqp.Connection
	maxConcurrency           int64
	controlStandardQueueName string
	controlPremiumQueueName  string
	computeStandardQueueName string
	computePremiumQueueName  string
}

func FromEnv() (*MessageQueueFactory, error) {
	host := os.Getenv("ITX_RABBITMQ_HOST")
	port := os.Getenv("ITX_RABBITMQ_PORT")
	user := os.Getenv("ITX_RABBITMQ_USER")
	password := os.Getenv("ITX_RABBITMQ_PASSWORD")

	url := fmt.Sprintf("amqp://%s:%s@%s:%s/%%2F", user, password, host, port)
	conn, err := amqp.Dial(url)
	if err != nil {
		return nil, err
	}

	maxConcurrency := int64(defaultMaxConcurrency)
	if raw := os.Getenv("ITX_RABBITMQ_MAX_CONCURRENCY"); raw != "" {
		if v, err := strconv.ParseInt(raw, 10, 64); err == nil && v > 0 {
			maxConcurrency = v
		}
	}

	return &MessageQueueFactory{
		conn:                     conn,
		maxConcurrency:           maxConcurrency,
		controlStandardQueueName: os.Getenv("ITX_RABBITMQ_CONTROL_STANDARD_QUEUE"),
		controlPremiumQueueName:  os.Getenv("ITX_RABBITMQ_CONTROL_PREMIUM_QUEUE"),
		computeStandardQueueName: os.Getenv("ITX_RABBITMQ_COMPUTE_STANDARD_QUEUE"),
		computePremiumQueueName:  os.Getenv("ITX_RABBITMQ_COMPUTE_PREMIUM_QUEUE"),
	}, nil
}

func (f *MessageQueueFactory) CreateControlStandardQueue() queue.MessageQueue {
	return newMessageQueue(f.conn, f.controlStandardQueueName, f.maxConcurrency)
}

func (f *MessageQueueFactory) CreateControlPremiumQueue() queue.MessageQueue {
	return newMessageQueue(f.conn, f.controlPremiumQueueName, f.maxConcurrency)
}

func (f *MessageQueueFactory) CreateComputeStandardQueue() queue.MessageQueue {
	return newMessageQueue(f.conn, f.computeStandardQueueName, f.maxConcurrency)
}

func (f *MessageQueueFactory) CreateComputePremiumQueue() queue.MessageQueue {
	return newMessageQueue(f.conn, f.computePremiumQueueName, f.maxConcurrency)
}
