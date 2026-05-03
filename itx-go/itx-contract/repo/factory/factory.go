package factory

import (
	"github.com/chehsunliu/itx/itx-go/itx-contract/repo/post"
	"github.com/chehsunliu/itx/itx-go/itx-contract/repo/subscription"
	"github.com/chehsunliu/itx/itx-go/itx-contract/repo/user"
)

type RepoFactory interface {
	CreatePostRepo() post.Repo
	CreateUserRepo() user.Repo
	CreateSubscriptionRepo() subscription.Repo
}
