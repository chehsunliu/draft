package post

import (
	"context"
	"errors"
	"time"

	"github.com/google/uuid"
)

var ErrNotFound = errors.New("not found")

type Post struct {
	ID         int64
	AuthorID   uuid.UUID
	Title      string
	Body       string
	Tags       []string
	CreatedAt  time.Time
	NotifiedAt *time.Time
}

type ListParams struct {
	AuthorID *uuid.UUID
	Limit    uint32
	Offset   uint32
}

type GetParams struct {
	ID int64
}

type CreateParams struct {
	AuthorID uuid.UUID
	Title    string
	Body     string
	Tags     []string
}

type UpdateParams struct {
	ID       int64
	AuthorID uuid.UUID
	Title    *string
	Body     *string
	Tags     *[]string
}

type DeleteParams struct {
	ID       int64
	AuthorID uuid.UUID
}

type Repo interface {
	List(ctx context.Context, params ListParams) ([]Post, error)
	Get(ctx context.Context, params GetParams) (Post, error)
	Create(ctx context.Context, params CreateParams) (Post, error)
	Update(ctx context.Context, params UpdateParams) (Post, error)
	Delete(ctx context.Context, params DeleteParams) error

	// MarkNotified stamps notified_at = now() for the given post. Idempotent — safe to retry.
	MarkNotified(ctx context.Context, id int64) error
}
