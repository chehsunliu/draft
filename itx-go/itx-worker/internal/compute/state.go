package compute

import (
	"fmt"
	"os"

	contractqueue "github.com/chehsunliu/itx/itx-go/itx-contract/queue"
	queuefactory "github.com/chehsunliu/itx/itx-go/itx-contract/queue/factory"
	queuerabbit "github.com/chehsunliu/itx/itx-go/itx-impl/queue/rabbitmq"
	queuesqs "github.com/chehsunliu/itx/itx-go/itx-impl/queue/sqs"
)

type WorkerState struct {
	ControlStandardQueue contractqueue.MessageQueue
	ControlPremiumQueue  contractqueue.MessageQueue
	ComputeStandardQueue contractqueue.MessageQueue
	ComputePremiumQueue  contractqueue.MessageQueue
}

func FromEnv() (WorkerState, error) {
	queueProvider := os.Getenv("ITX_QUEUE_PROVIDER")
	if queueProvider == "" {
		queueProvider = "sqs"
	}
	var queueFactory queuefactory.MessageQueueFactory
	switch queueProvider {
	case "sqs":
		f, err := queuesqs.FromEnv()
		if err != nil {
			return WorkerState{}, err
		}
		queueFactory = f
	case "rabbitmq":
		f, err := queuerabbit.FromEnv()
		if err != nil {
			return WorkerState{}, err
		}
		queueFactory = f
	default:
		return WorkerState{}, fmt.Errorf("unknown ITX_QUEUE_PROVIDER: %s", queueProvider)
	}

	return WorkerState{
		ControlStandardQueue: queueFactory.CreateControlStandardQueue(),
		ControlPremiumQueue:  queueFactory.CreateControlPremiumQueue(),
		ComputeStandardQueue: queueFactory.CreateComputeStandardQueue(),
		ComputePremiumQueue:  queueFactory.CreateComputePremiumQueue(),
	}, nil
}
