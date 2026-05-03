package factory

import (
	"github.com/chehsunliu/itx/itx-go/itx-contract/queue"
)

type MessageQueueFactory interface {
	CreateControlStandardQueue() queue.MessageQueue
	CreateControlPremiumQueue() queue.MessageQueue
	CreateComputeStandardQueue() queue.MessageQueue
	CreateComputePremiumQueue() queue.MessageQueue
}
