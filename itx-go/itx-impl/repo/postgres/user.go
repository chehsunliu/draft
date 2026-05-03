package postgres

import (
	"context"
	"database/sql"

	"github.com/chehsunliu/itx/itx-go/itx-contract/repo/user"
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
