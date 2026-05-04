package main

import (
	"log"

	contractqueue "github.com/chehsunliu/itx/itx-go/itx-contract/queue"
	"github.com/chehsunliu/itx/itx-go/itx-worker/internal/compute"
	"github.com/chehsunliu/itx/itx-go/itx-worker/internal/run"
)

func main() {
	state, err := compute.FromEnv()
	if err != nil {
		log.Fatal(err)
	}
	queues := []contractqueue.MessageQueue{state.ComputeStandardQueue, state.ComputePremiumQueue}
	run.Run(queues, compute.NewDispatcher(state))
}
