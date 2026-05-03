package subscription

import (
	"context"

	"github.com/chehsunliu/itx/itx-go/itx-contract/repo/user"
	"github.com/google/uuid"
)

type SubscribeParams struct {
	SubscriberID uuid.UUID
	AuthorID     uuid.UUID
}

type UnsubscribeParams struct {
	SubscriberID uuid.UUID
	AuthorID     uuid.UUID
}

type Repo interface {
	Subscribe(ctx context.Context, params SubscribeParams) error
	Unsubscribe(ctx context.Context, params UnsubscribeParams) error

	// ListAuthors returns the users that subscriberID follows, ordered by most recently
	// subscribed first.
	ListAuthors(ctx context.Context, subscriberID uuid.UUID) ([]user.User, error)
}
