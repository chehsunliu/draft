package state

import (
	"fmt"
	"os"

	"github.com/chehsunliu/itx/itx-go/itx-contract/queue"
	queuefactory "github.com/chehsunliu/itx/itx-go/itx-contract/queue/factory"
	"github.com/chehsunliu/itx/itx-go/itx-contract/repo/factory"
	"github.com/chehsunliu/itx/itx-go/itx-contract/repo/post"
	"github.com/chehsunliu/itx/itx-go/itx-contract/repo/subscription"
	"github.com/chehsunliu/itx/itx-go/itx-contract/repo/user"
	queuerabbit "github.com/chehsunliu/itx/itx-go/itx-impl/queue/rabbitmq"
	queuesqs "github.com/chehsunliu/itx/itx-go/itx-impl/queue/sqs"
	"github.com/chehsunliu/itx/itx-go/itx-impl/repo/mariadb"
	"github.com/chehsunliu/itx/itx-go/itx-impl/repo/postgres"
)

type AppState struct {
	PostRepo             post.Repo
	UserRepo             user.Repo
	SubscriptionRepo     subscription.Repo
	ControlStandardQueue queue.MessageQueue
}

func FromEnv() (AppState, error) {
	provider := os.Getenv("ITX_DB_PROVIDER")
	if provider == "" {
		provider = "postgres"
	}

	var repoFactory factory.RepoFactory
	switch provider {
	case "postgres":
		f, err := postgres.FromEnv()
		if err != nil {
			return AppState{}, err
		}
		repoFactory = f
	case "mariadb":
		f, err := mariadb.FromEnv()
		if err != nil {
			return AppState{}, err
		}
		repoFactory = f
	default:
		return AppState{}, fmt.Errorf("unknown ITX_DB_PROVIDER: %s", provider)
	}

	queueProvider := os.Getenv("ITX_QUEUE_PROVIDER")
	if queueProvider == "" {
		queueProvider = "sqs"
	}

	var queueFactory queuefactory.MessageQueueFactory
	switch queueProvider {
	case "sqs":
		f, err := queuesqs.FromEnv()
		if err != nil {
			return AppState{}, err
		}
		queueFactory = f
	case "rabbitmq":
		f, err := queuerabbit.FromEnv()
		if err != nil {
			return AppState{}, err
		}
		queueFactory = f
	default:
		return AppState{}, fmt.Errorf("unknown ITX_QUEUE_PROVIDER: %s", queueProvider)
	}

	return AppState{
		PostRepo:             repoFactory.CreatePostRepo(),
		UserRepo:             repoFactory.CreateUserRepo(),
		SubscriptionRepo:     repoFactory.CreateSubscriptionRepo(),
		ControlStandardQueue: queueFactory.CreateControlStandardQueue(),
	}, nil
}
