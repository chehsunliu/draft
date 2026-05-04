package main

import (
	"log"

	contractqueue "github.com/chehsunliu/itx/itx-go/itx-contract/queue"
	"github.com/chehsunliu/itx/itx-go/itx-worker/internal/control"
	"github.com/chehsunliu/itx/itx-go/itx-worker/internal/run"
)

func main() {
	state, err := control.FromEnv()
	if err != nil {
		log.Fatal(err)
	}
	queues := []contractqueue.MessageQueue{state.ControlStandardQueue, state.ControlPremiumQueue}
	run.Run(queues, control.NewDispatcher(state))
}
