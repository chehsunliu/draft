package postgres

import (
	"context"
	"database/sql"
	"errors"
	"strconv"
	"strings"
	"time"

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
			"SELECT id, author_id, title, body, created_at, notified_at "+
				"FROM posts WHERE author_id = $1 "+
				"ORDER BY id DESC LIMIT $2 OFFSET $3",
			params.AuthorID.String(), limit, offset)
	} else {
		rows, err = r.db.QueryContext(ctx,
			"SELECT id, author_id, title, body, created_at, notified_at "+
				"FROM posts ORDER BY id DESC LIMIT $1 OFFSET $2",
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
		var notifiedAt sql.NullTime
		if err := rows.Scan(&p.ID, &authorID, &p.Title, &p.Body, &p.CreatedAt, &notifiedAt); err != nil {
			return nil, err
		}
		if err := p.AuthorID.UnmarshalText([]byte(authorID)); err != nil {
			return nil, err
		}
		if notifiedAt.Valid {
			t := notifiedAt.Time
			p.NotifiedAt = &t
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

func (r *postRepo) Get(ctx context.Context, params post.GetParams) (post.Post, error) {
	row := r.db.QueryRowContext(ctx,
		"SELECT id, author_id, title, body, created_at, notified_at FROM posts WHERE id = $1",
		params.ID)
	var p post.Post
	var authorID string
	var notifiedAt sql.NullTime
	if err := row.Scan(&p.ID, &authorID, &p.Title, &p.Body, &p.CreatedAt, &notifiedAt); err != nil {
		if errors.Is(err, sql.ErrNoRows) {
			return post.Post{}, post.ErrNotFound
		}
		return post.Post{}, err
	}
	if err := p.AuthorID.UnmarshalText([]byte(authorID)); err != nil {
		return post.Post{}, err
	}
	if notifiedAt.Valid {
		t := notifiedAt.Time
		p.NotifiedAt = &t
	}

	tagMap, err := fetchTagsFor(ctx, r.db, []int64{p.ID})
	if err != nil {
		return post.Post{}, err
	}
	p.Tags = tagMap[p.ID]
	if p.Tags == nil {
		p.Tags = []string{}
	}
	return p, nil
}

func (r *postRepo) Create(ctx context.Context, params post.CreateParams) (post.Post, error) {
	tx, err := r.db.BeginTx(ctx, nil)
	if err != nil {
		return post.Post{}, err
	}
	defer tx.Rollback()

	var id int64
	var createdAt time.Time
	err = tx.QueryRowContext(ctx,
		"INSERT INTO posts (author_id, title, body) VALUES ($1, $2, $3) RETURNING id, created_at",
		params.AuthorID.String(), params.Title, params.Body).Scan(&id, &createdAt)
	if err != nil {
		return post.Post{}, err
	}

	tagIDs, err := upsertTagsTx(ctx, tx, params.Tags)
	if err != nil {
		return post.Post{}, err
	}
	if err := linkPostTagsTx(ctx, tx, id, tagIDs); err != nil {
		return post.Post{}, err
	}

	if err := tx.Commit(); err != nil {
		return post.Post{}, err
	}

	tags := append([]string{}, params.Tags...)
	return post.Post{
		ID:        id,
		AuthorID:  params.AuthorID,
		Title:     params.Title,
		Body:      params.Body,
		Tags:      tags,
		CreatedAt: createdAt,
	}, nil
}

func (r *postRepo) Update(ctx context.Context, params post.UpdateParams) (post.Post, error) {
	tx, err := r.db.BeginTx(ctx, nil)
	if err != nil {
		return post.Post{}, err
	}
	defer tx.Rollback()

	var p post.Post
	var authorID string
	var notifiedAt sql.NullTime
	err = tx.QueryRowContext(ctx,
		"SELECT id, author_id, title, body, created_at, notified_at FROM posts "+
			"WHERE id = $1 AND author_id = $2 FOR UPDATE",
		params.ID, params.AuthorID.String()).Scan(&p.ID, &authorID, &p.Title, &p.Body, &p.CreatedAt, &notifiedAt)
	if err != nil {
		if errors.Is(err, sql.ErrNoRows) {
			return post.Post{}, post.ErrNotFound
		}
		return post.Post{}, err
	}
	p.AuthorID = params.AuthorID
	if notifiedAt.Valid {
		t := notifiedAt.Time
		p.NotifiedAt = &t
	}

	if params.Title != nil {
		p.Title = *params.Title
	}
	if params.Body != nil {
		p.Body = *params.Body
	}
	if _, err := tx.ExecContext(ctx,
		"UPDATE posts SET title = $1, body = $2 WHERE id = $3",
		p.Title, p.Body, p.ID); err != nil {
		return post.Post{}, err
	}

	if params.Tags != nil {
		if _, err := tx.ExecContext(ctx, "DELETE FROM post_tags WHERE post_id = $1", p.ID); err != nil {
			return post.Post{}, err
		}
		tagIDs, err := upsertTagsTx(ctx, tx, *params.Tags)
		if err != nil {
			return post.Post{}, err
		}
		if err := linkPostTagsTx(ctx, tx, p.ID, tagIDs); err != nil {
			return post.Post{}, err
		}
		p.Tags = append([]string{}, (*params.Tags)...)
	} else {
		rows, err := tx.QueryContext(ctx,
			"SELECT t.name FROM post_tags pt JOIN tags t ON pt.tag_id = t.id "+
				"WHERE pt.post_id = $1 ORDER BY t.name", p.ID)
		if err != nil {
			return post.Post{}, err
		}
		var tags []string
		for rows.Next() {
			var name string
			if err := rows.Scan(&name); err != nil {
				rows.Close()
				return post.Post{}, err
			}
			tags = append(tags, name)
		}
		rows.Close()
		if tags == nil {
			tags = []string{}
		}
		p.Tags = tags
	}

	if err := tx.Commit(); err != nil {
		return post.Post{}, err
	}
	return p, nil
}

func (r *postRepo) Delete(ctx context.Context, params post.DeleteParams) error {
	result, err := r.db.ExecContext(ctx,
		"DELETE FROM posts WHERE id = $1 AND author_id = $2",
		params.ID, params.AuthorID.String())
	if err != nil {
		return err
	}
	rows, err := result.RowsAffected()
	if err != nil {
		return err
	}
	if rows == 0 {
		return post.ErrNotFound
	}
	return nil
}

func (r *postRepo) MarkNotified(ctx context.Context, id int64) error {
	_, err := r.db.ExecContext(ctx,
		"UPDATE posts SET notified_at = now() WHERE id = $1", id)
	return err
}

func upsertTagsTx(ctx context.Context, tx *sql.Tx, names []string) ([]int64, error) {
	ids := make([]int64, 0, len(names))
	for _, name := range names {
		var id int64
		if err := tx.QueryRowContext(ctx,
			"INSERT INTO tags (name) VALUES ($1) "+
				"ON CONFLICT (name) DO UPDATE SET name = EXCLUDED.name "+
				"RETURNING id", name).Scan(&id); err != nil {
			return nil, err
		}
		ids = append(ids, id)
	}
	return ids, nil
}

func linkPostTagsTx(ctx context.Context, tx *sql.Tx, postID int64, tagIDs []int64) error {
	for _, tagID := range tagIDs {
		if _, err := tx.ExecContext(ctx,
			"INSERT INTO post_tags (post_id, tag_id) VALUES ($1, $2) ON CONFLICT DO NOTHING",
			postID, tagID); err != nil {
			return err
		}
	}
	return nil
}

func fetchTagsFor(ctx context.Context, db *sql.DB, ids []int64) (map[int64][]string, error) {
	out := map[int64][]string{}
	if len(ids) == 0 {
		return out, nil
	}
	placeholders := make([]string, len(ids))
	args := make([]any, len(ids))
	for i, id := range ids {
		placeholders[i] = "$" + strconv.Itoa(i+1)
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
