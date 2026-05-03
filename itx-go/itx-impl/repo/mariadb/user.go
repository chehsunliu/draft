package mariadb

import (
	"context"
	"database/sql"

	"github.com/chehsunliu/itx/itx-go/itx-contract/repo/user"
)

type userRepo struct {
	db *sql.DB
}

func (r *userRepo) Upsert(ctx context.Context, params user.UpsertParams) (user.User, error) {
	if _, err := r.db.ExecContext(ctx,
		"INSERT INTO users (id, email) VALUES (?, ?) ON DUPLICATE KEY UPDATE id = id",
		params.ID.String(), params.Email); err != nil {
		return user.User{}, err
	}

	var u user.User
	var idStr string
	if err := r.db.QueryRowContext(ctx,
		"SELECT id, email FROM users WHERE id = ?", params.ID.String()).Scan(&idStr, &u.Email); err != nil {
		return user.User{}, err
	}
	if err := u.ID.UnmarshalText([]byte(idStr)); err != nil {
		return user.User{}, err
	}
	return u, nil
}
