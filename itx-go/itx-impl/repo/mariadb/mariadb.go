package mariadb

import (
	"database/sql"
	"fmt"
	"os"

	"github.com/chehsunliu/itx/itx-go/itx-contract/repo/post"
	"github.com/chehsunliu/itx/itx-go/itx-contract/repo/user"
	_ "github.com/go-sql-driver/mysql"
)

type RepoFactory struct {
	db *sql.DB
}

func FromEnv() (*RepoFactory, error) {
	host := os.Getenv("ITX_MARIADB_HOST")
	port := os.Getenv("ITX_MARIADB_PORT")
	dbName := os.Getenv("ITX_MARIADB_DB_NAME")
	dbUser := os.Getenv("ITX_MARIADB_USER")
	password := os.Getenv("ITX_MARIADB_PASSWORD")

	dsn := fmt.Sprintf("%s:%s@tcp(%s:%s)/%s?parseTime=true&loc=UTC",
		dbUser, password, host, port, dbName)
	db, err := sql.Open("mysql", dsn)
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
