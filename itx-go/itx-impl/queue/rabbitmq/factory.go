package rabbitmq

import (
	"fmt"
	"os"

	"github.com/chehsunliu/itx/itx-go/itx-contract/queue"
	amqp "github.com/rabbitmq/amqp091-go"
)

type MessageQueueFactory struct {
	conn                     *amqp.Connection
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

	return &MessageQueueFactory{
		conn:                     conn,
		controlStandardQueueName: os.Getenv("ITX_RABBITMQ_CONTROL_STANDARD_QUEUE"),
		controlPremiumQueueName:  os.Getenv("ITX_RABBITMQ_CONTROL_PREMIUM_QUEUE"),
		computeStandardQueueName: os.Getenv("ITX_RABBITMQ_COMPUTE_STANDARD_QUEUE"),
		computePremiumQueueName:  os.Getenv("ITX_RABBITMQ_COMPUTE_PREMIUM_QUEUE"),
	}, nil
}

func (f *MessageQueueFactory) CreateControlStandardQueue() queue.MessageQueue {
	return newMessageQueue(f.conn, f.controlStandardQueueName)
}

func (f *MessageQueueFactory) CreateControlPremiumQueue() queue.MessageQueue {
	return newMessageQueue(f.conn, f.controlPremiumQueueName)
}

func (f *MessageQueueFactory) CreateComputeStandardQueue() queue.MessageQueue {
	return newMessageQueue(f.conn, f.computeStandardQueueName)
}

func (f *MessageQueueFactory) CreateComputePremiumQueue() queue.MessageQueue {
	return newMessageQueue(f.conn, f.computePremiumQueueName)
}
