package mariadb

import (
	"context"
	"database/sql"
	"errors"
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
				"FROM posts WHERE author_id = ? "+
				"ORDER BY id DESC LIMIT ? OFFSET ?",
			params.AuthorID.String(), limit, offset)
	} else {
		rows, err = r.db.QueryContext(ctx,
			"SELECT id, author_id, title, body, created_at, notified_at "+
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
		"SELECT id, author_id, title, body, created_at, notified_at FROM posts WHERE id = ?",
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

	result, err := tx.ExecContext(ctx,
		"INSERT INTO posts (author_id, title, body) VALUES (?, ?, ?)",
		params.AuthorID.String(), params.Title, params.Body)
	if err != nil {
		return post.Post{}, err
	}
	id, err := result.LastInsertId()
	if err != nil {
		return post.Post{}, err
	}

	var createdAt time.Time
	if err := tx.QueryRowContext(ctx,
		"SELECT created_at FROM posts WHERE id = ?", id).Scan(&createdAt); err != nil {
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
			"WHERE id = ? AND author_id = ? FOR UPDATE",
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
		"UPDATE posts SET title = ?, body = ? WHERE id = ?",
		p.Title, p.Body, p.ID); err != nil {
		return post.Post{}, err
	}

	if params.Tags != nil {
		if _, err := tx.ExecContext(ctx, "DELETE FROM post_tags WHERE post_id = ?", p.ID); err != nil {
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
				"WHERE pt.post_id = ? ORDER BY t.name", p.ID)
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
		"DELETE FROM posts WHERE id = ? AND author_id = ?",
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
		"UPDATE posts SET notified_at = NOW() WHERE id = ?", id)
	return err
}

func upsertTagsTx(ctx context.Context, tx *sql.Tx, names []string) ([]int64, error) {
	ids := make([]int64, 0, len(names))
	for _, name := range names {
		if _, err := tx.ExecContext(ctx, "INSERT IGNORE INTO tags (name) VALUES (?)", name); err != nil {
			return nil, err
		}
		var id int64
		if err := tx.QueryRowContext(ctx, "SELECT id FROM tags WHERE name = ?", name).Scan(&id); err != nil {
			return nil, err
		}
		ids = append(ids, id)
	}
	return ids, nil
}

func linkPostTagsTx(ctx context.Context, tx *sql.Tx, postID int64, tagIDs []int64) error {
	for _, tagID := range tagIDs {
		if _, err := tx.ExecContext(ctx,
			"INSERT IGNORE INTO post_tags (post_id, tag_id) VALUES (?, ?)",
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
