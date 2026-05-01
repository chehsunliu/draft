package post

import (
	"context"
	"errors"
	"time"

	"github.com/google/uuid"
)

var ErrNotFound = errors.New("not found")

type Post struct {
	ID        int64
	AuthorID  uuid.UUID
	Title     string
	Body      string
	Tags      []string
	CreatedAt time.Time
}

type ListParams struct {
	AuthorID *uuid.UUID
	Limit    uint32
	Offset   uint32
}

type Repo interface {
	List(ctx context.Context, params ListParams) ([]Post, error)
}
