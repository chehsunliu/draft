package user

import (
	"context"

	"github.com/google/uuid"
)

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
}
