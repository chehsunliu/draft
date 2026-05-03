package mariadb

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
		"INSERT IGNORE INTO subscriptions (subscriber_id, author_id) VALUES (?, ?)",
		params.SubscriberID.String(), params.AuthorID.String())
	return err
}

func (r *subscriptionRepo) Unsubscribe(ctx context.Context, params subscription.UnsubscribeParams) error {
	_, err := r.db.ExecContext(ctx,
		"DELETE FROM subscriptions WHERE subscriber_id = ? AND author_id = ?",
		params.SubscriberID.String(), params.AuthorID.String())
	return err
}
