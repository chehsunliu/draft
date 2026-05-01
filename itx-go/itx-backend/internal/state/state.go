package state

import (
	"fmt"
	"os"

	"github.com/chehsunliu/itx/itx-go/itx-contract/repo/factory"
	"github.com/chehsunliu/itx/itx-go/itx-contract/repo/post"
	"github.com/chehsunliu/itx/itx-go/itx-impl/repo/mariadb"
	"github.com/chehsunliu/itx/itx-go/itx-impl/repo/postgres"
)

type AppState struct {
	PostRepo post.Repo
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

	return AppState{PostRepo: repoFactory.CreatePostRepo()}, nil
}
