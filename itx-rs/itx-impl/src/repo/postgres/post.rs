use std::collections::HashMap;

use async_trait::async_trait;
use itx_contract::repo::error::RepoError;
use itx_contract::repo::post::{
    AuthorId, CreateParams, DeleteParams, GetParams, ListParams, Post, PostId, PostRepo, UpdateParams,
};
use sqlx::{PgPool, Postgres, Transaction};

pub struct PostgresPostRepo {
    pool: PgPool,
}

impl PostgresPostRepo {
    pub fn new(pool: PgPool) -> Self {
        Self { pool }
    }
}

fn err<E: std::fmt::Display>(e: E) -> RepoError {
    RepoError::Unknown(e.to_string())
}

async fn upsert_tags(tx: &mut Transaction<'_, Postgres>, names: &[String]) -> Result<Vec<i64>, RepoError> {
    let mut ids = Vec::with_capacity(names.len());
    for name in names {
        let id: i64 = sqlx::query_scalar(
            "INSERT INTO tags (name) VALUES ($1) \
             ON CONFLICT (name) DO UPDATE SET name = EXCLUDED.name \
             RETURNING id",
        )
        .bind(name)
        .fetch_one(&mut **tx)
        .await
        .map_err(err)?;
        ids.push(id);
    }
    Ok(ids)
}

async fn link_post_tags(tx: &mut Transaction<'_, Postgres>, post_id: PostId, tag_ids: &[i64]) -> Result<(), RepoError> {
    for tid in tag_ids {
        sqlx::query("INSERT INTO post_tags (post_id, tag_id) VALUES ($1, $2) ON CONFLICT DO NOTHING")
            .bind(post_id)
            .bind(tid)
            .execute(&mut **tx)
            .await
            .map_err(err)?;
    }
    Ok(())
}

async fn fetch_tags_for(pool: &PgPool, post_ids: &[PostId]) -> Result<HashMap<PostId, Vec<String>>, RepoError> {
    if post_ids.is_empty() {
        return Ok(HashMap::new());
    }
    let rows: Vec<(PostId, String)> = sqlx::query_as(
        "SELECT pt.post_id, t.name \
         FROM post_tags pt JOIN tags t ON pt.tag_id = t.id \
         WHERE pt.post_id = ANY($1) \
         ORDER BY t.name",
    )
    .bind(post_ids)
    .fetch_all(pool)
    .await
    .map_err(err)?;

    let mut map: HashMap<PostId, Vec<String>> = HashMap::new();
    for (pid, name) in rows {
        map.entry(pid).or_default().push(name);
    }
    Ok(map)
}

#[async_trait]
impl PostRepo for PostgresPostRepo {
    async fn list(&self, params: ListParams) -> Result<Vec<Post>, RepoError> {
        let limit = if params.limit == 0 { 50 } else { params.limit as i64 };
        let offset = params.offset as i64;

        let rows: Vec<(PostId, AuthorId, String, String, time::OffsetDateTime)> = match params.author_id {
            Some(author_id) => sqlx::query_as(
                "SELECT id, author_id, title, body, created_at \
                     FROM posts WHERE author_id = $1 \
                     ORDER BY id DESC LIMIT $2 OFFSET $3",
            )
            .bind(author_id)
            .bind(limit)
            .bind(offset)
            .fetch_all(&self.pool)
            .await
            .map_err(err)?,
            None => sqlx::query_as(
                "SELECT id, author_id, title, body, created_at \
                     FROM posts ORDER BY id DESC LIMIT $1 OFFSET $2",
            )
            .bind(limit)
            .bind(offset)
            .fetch_all(&self.pool)
            .await
            .map_err(err)?,
        };

        let ids: Vec<PostId> = rows.iter().map(|r| r.0).collect();
        let mut tag_map = fetch_tags_for(&self.pool, &ids).await?;

        Ok(rows
            .into_iter()
            .map(|(id, author_id, title, body, created_at)| Post {
                id,
                author_id,
                title,
                body,
                tags: tag_map.remove(&id).unwrap_or_default(),
                created_at,
            })
            .collect())
    }

    async fn get(&self, params: GetParams) -> Result<Post, RepoError> {
        let row: Option<(PostId, AuthorId, String, String, time::OffsetDateTime)> =
            sqlx::query_as("SELECT id, author_id, title, body, created_at FROM posts WHERE id = $1")
                .bind(params.id)
                .fetch_optional(&self.pool)
                .await
                .map_err(err)?;

        let Some((id, author_id, title, body, created_at)) = row else {
            return Err(RepoError::NotFound);
        };
        let mut tag_map = fetch_tags_for(&self.pool, &[id]).await?;
        Ok(Post {
            id,
            author_id,
            title,
            body,
            tags: tag_map.remove(&id).unwrap_or_default(),
            created_at,
        })
    }

    async fn create(&self, params: CreateParams) -> Result<Post, RepoError> {
        let mut tx = self.pool.begin().await.map_err(err)?;

        let (id, created_at): (PostId, time::OffsetDateTime) = sqlx::query_as(
            "INSERT INTO posts (author_id, title, body) VALUES ($1, $2, $3) \
             RETURNING id, created_at",
        )
        .bind(params.author_id)
        .bind(&params.title)
        .bind(&params.body)
        .fetch_one(&mut *tx)
        .await
        .map_err(err)?;

        let tag_ids = upsert_tags(&mut tx, &params.tags).await?;
        link_post_tags(&mut tx, id, &tag_ids).await?;

        tx.commit().await.map_err(err)?;

        Ok(Post {
            id,
            author_id: params.author_id,
            title: params.title,
            body: params.body,
            tags: params.tags,
            created_at,
        })
    }

    async fn update(&self, params: UpdateParams) -> Result<Post, RepoError> {
        let mut tx = self.pool.begin().await.map_err(err)?;

        let existing: Option<(PostId, AuthorId, String, String, time::OffsetDateTime)> = sqlx::query_as(
            "SELECT id, author_id, title, body, created_at \
                 FROM posts WHERE id = $1 AND author_id = $2 FOR UPDATE",
        )
        .bind(params.id)
        .bind(params.author_id)
        .fetch_optional(&mut *tx)
        .await
        .map_err(err)?;

        let Some((_, _, mut title, mut body, created_at)) = existing else {
            return Err(RepoError::NotFound);
        };

        if let Some(t) = params.title {
            title = t;
        }
        if let Some(b) = params.body {
            body = b;
        }

        sqlx::query("UPDATE posts SET title = $1, body = $2 WHERE id = $3")
            .bind(&title)
            .bind(&body)
            .bind(params.id)
            .execute(&mut *tx)
            .await
            .map_err(err)?;

        let tags = if let Some(new_tags) = params.tags {
            sqlx::query("DELETE FROM post_tags WHERE post_id = $1")
                .bind(params.id)
                .execute(&mut *tx)
                .await
                .map_err(err)?;
            let tag_ids = upsert_tags(&mut tx, &new_tags).await?;
            link_post_tags(&mut tx, params.id, &tag_ids).await?;
            new_tags
        } else {
            let rows: Vec<(String,)> = sqlx::query_as(
                "SELECT t.name FROM post_tags pt JOIN tags t ON pt.tag_id = t.id \
                 WHERE pt.post_id = $1 ORDER BY t.name",
            )
            .bind(params.id)
            .fetch_all(&mut *tx)
            .await
            .map_err(err)?;
            rows.into_iter().map(|r| r.0).collect()
        };

        tx.commit().await.map_err(err)?;

        Ok(Post {
            id: params.id,
            author_id: params.author_id,
            title,
            body,
            tags,
            created_at,
        })
    }

    async fn delete(&self, params: DeleteParams) -> Result<(), RepoError> {
        let result = sqlx::query("DELETE FROM posts WHERE id = $1 AND author_id = $2")
            .bind(params.id)
            .bind(params.author_id)
            .execute(&self.pool)
            .await
            .map_err(err)?;
        if result.rows_affected() == 0 {
            return Err(RepoError::NotFound);
        }
        Ok(())
    }
}
