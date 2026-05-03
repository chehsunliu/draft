package user

import (
	"context"
	"errors"

	"github.com/google/uuid"
)

var ErrNotFound = errors.New("not found")

type User struct {
	ID    uuid.UUID
	Email string
}

type UpsertParams struct {
	ID    uuid.UUID
	Email string
}

type Repo interface {
	Upsert(ctx context.Context, params UpsertParams) (User, error)
	Get(ctx context.Context, id uuid.UUID) (User, error)
}
