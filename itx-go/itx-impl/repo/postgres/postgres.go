package postgres

import (
	"database/sql"
	"fmt"
	"os"

	"github.com/chehsunliu/itx/itx-go/itx-contract/repo/post"
	"github.com/chehsunliu/itx/itx-go/itx-contract/repo/user"
	_ "github.com/jackc/pgx/v5/stdlib"
)

type RepoFactory struct {
	db *sql.DB
}

func FromEnv() (*RepoFactory, error) {
	host := os.Getenv("ITX_POSTGRES_HOST")
	port := os.Getenv("ITX_POSTGRES_PORT")
	dbName := os.Getenv("ITX_POSTGRES_DB_NAME")
	dbUser := os.Getenv("ITX_POSTGRES_USER")
	password := os.Getenv("ITX_POSTGRES_PASSWORD")

	dsn := fmt.Sprintf("postgres://%s:%s@%s:%s/%s", dbUser, password, host, port, dbName)
	db, err := sql.Open("pgx", dsn)
	if err != nil {
		return nil, err
	}
	db.SetMaxOpenConns(10)

	return &RepoFactory{db: db}, nil
}

func (f *RepoFactory) CreatePostRepo() post.Repo {
	return &postRepo{db: f.db}
}

func (f *RepoFactory) CreateUserRepo() user.Repo {
	return &userRepo{db: f.db}
}
