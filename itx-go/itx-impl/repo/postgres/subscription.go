package postgres

import (
	"context"
	"database/sql"

	"github.com/chehsunliu/itx/itx-go/itx-contract/repo/subscription"
	"github.com/chehsunliu/itx/itx-go/itx-contract/repo/user"
	"github.com/google/uuid"
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

func (r *subscriptionRepo) ListAuthors(ctx context.Context, subscriberID uuid.UUID) ([]user.User, error) {
	rows, err := r.db.QueryContext(ctx,
		"SELECT u.id, u.email "+
			"FROM subscriptions s JOIN users u ON u.id = s.author_id "+
			"WHERE s.subscriber_id = $1 "+
			"ORDER BY s.created_at DESC, u.id ASC",
		subscriberID.String())
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	out := []user.User{}
	for rows.Next() {
		var u user.User
		var idStr string
		if err := rows.Scan(&idStr, &u.Email); err != nil {
			return nil, err
		}
		if err := u.ID.UnmarshalText([]byte(idStr)); err != nil {
			return nil, err
		}
		out = append(out, u)
	}
	return out, rows.Err()
}
