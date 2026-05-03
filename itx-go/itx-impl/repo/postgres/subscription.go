package postgres

import (
	"context"
	"database/sql"

	"github.com/chehsunliu/itx/itx-go/itx-contract/repo/subscription"
)

type subscriptionRepo struct {
	db *sql.DB
}

func (r *subscriptionRepo) Subscribe(ctx context.Context, params subscription.SubscribeParams) error {
	_, err := r.db.ExecContext(ctx,
		"INSERT INTO subscriptions (subscriber_id, author_id) VALUES ($1, $2) "+
			"ON CONFLICT (subscriber_id, author_id) DO NOTHING",
		params.SubscriberID.String(), params.AuthorID.String())
	return err
}

func (r *subscriptionRepo) Unsubscribe(ctx context.Context, params subscription.UnsubscribeParams) error {
	_, err := r.db.ExecContext(ctx,
		"DELETE FROM subscriptions WHERE subscriber_id = $1 AND author_id = $2",
		params.SubscriberID.String(), params.AuthorID.String())
	return err
}
