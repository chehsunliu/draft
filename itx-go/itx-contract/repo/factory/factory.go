package factory

import (
	"github.com/chehsunliu/itx/itx-go/itx-contract/repo/post"
)

type RepoFactory interface {
	CreatePostRepo() post.Repo
}
