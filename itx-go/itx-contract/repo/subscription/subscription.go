package subscription

import (
	"context"

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
}
