package mariadb

import (
	"context"
	"database/sql"
	"strings"

	"github.com/chehsunliu/itx/itx-go/itx-contract/repo/post"
)

type postRepo struct {
	db *sql.DB
}

func (r *postRepo) List(ctx context.Context, params post.ListParams) ([]post.Post, error) {
	limit := int64(params.Limit)
	if limit == 0 {
		limit = 50
	}
	offset := int64(params.Offset)

	var (
		rows *sql.Rows
		err  error
	)
	if params.AuthorID != nil {
		rows, err = r.db.QueryContext(ctx,
			"SELECT id, author_id, title, body, created_at "+
				"FROM posts WHERE author_id = ? "+
				"ORDER BY id DESC LIMIT ? OFFSET ?",
			params.AuthorID.String(), limit, offset)
	} else {
		rows, err = r.db.QueryContext(ctx,
			"SELECT id, author_id, title, body, created_at "+
				"FROM posts ORDER BY id DESC LIMIT ? OFFSET ?",
			limit, offset)
	}
	if err != nil {
		return nil, err
	}
	defer rows.Close()

	var posts []post.Post
	var ids []int64
	for rows.Next() {
		var p post.Post
		var authorID string
		if err := rows.Scan(&p.ID, &authorID, &p.Title, &p.Body, &p.CreatedAt); err != nil {
			return nil, err
		}
		if err := p.AuthorID.UnmarshalText([]byte(authorID)); err != nil {
			return nil, err
		}
		posts = append(posts, p)
		ids = append(ids, p.ID)
	}
	if err := rows.Err(); err != nil {
		return nil, err
	}

	tagMap, err := fetchTagsFor(ctx, r.db, ids)
	if err != nil {
		return nil, err
	}
	for i := range posts {
		posts[i].Tags = tagMap[posts[i].ID]
		if posts[i].Tags == nil {
			posts[i].Tags = []string{}
		}
	}
	return posts, nil
}

func fetchTagsFor(ctx context.Context, db *sql.DB, ids []int64) (map[int64][]string, error) {
	out := map[int64][]string{}
	if len(ids) == 0 {
		return out, nil
	}
	placeholders := make([]string, len(ids))
	args := make([]any, len(ids))
	for i, id := range ids {
		placeholders[i] = "?"
		args[i] = id
	}
	rows, err := db.QueryContext(ctx,
		"SELECT pt.post_id, t.name FROM post_tags pt JOIN tags t ON pt.tag_id = t.id "+
			"WHERE pt.post_id IN ("+strings.Join(placeholders, ",")+") ORDER BY t.name",
		args...)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	for rows.Next() {
		var pid int64
		var name string
		if err := rows.Scan(&pid, &name); err != nil {
			return nil, err
		}
		out[pid] = append(out[pid], name)
	}
	return out, rows.Err()
}
