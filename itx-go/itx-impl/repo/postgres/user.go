package postgres

import (
	"context"
	"database/sql"
	"errors"

	"github.com/chehsunliu/itx/itx-go/itx-contract/repo/user"
	"github.com/google/uuid"
)

type userRepo struct {
	db *sql.DB
}

func (r *userRepo) Upsert(ctx context.Context, params user.UpsertParams) (user.User, error) {
	var u user.User
	var idStr string
	err := r.db.QueryRowContext(ctx,
		"INSERT INTO users (id, email) VALUES ($1, $2) "+
			"ON CONFLICT (id) DO UPDATE SET id = EXCLUDED.id "+
			"RETURNING id, email",
		params.ID.String(), params.Email).Scan(&idStr, &u.Email)
	if err != nil {
		return user.User{}, err
	}
	if err := u.ID.UnmarshalText([]byte(idStr)); err != nil {
		return user.User{}, err
	}
	return u, nil
}

func (r *userRepo) Get(ctx context.Context, id uuid.UUID) (user.User, error) {
	var u user.User
	var idStr string
	err := r.db.QueryRowContext(ctx,
		"SELECT id, email FROM users WHERE id = $1", id.String()).Scan(&idStr, &u.Email)
	if err != nil {
		if errors.Is(err, sql.ErrNoRows) {
			return user.User{}, user.ErrNotFound
		}
		return user.User{}, err
	}
	if err := u.ID.UnmarshalText([]byte(idStr)); err != nil {
		return user.User{}, err
	}
	return u, nil
}
