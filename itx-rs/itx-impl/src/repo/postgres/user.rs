use async_trait::async_trait;
use itx_contract::repo::error::RepoError;
use itx_contract::repo::user::{UpsertParams, User, UserRepo};
use sqlx::PgPool;
use uuid::Uuid;

pub struct PostgresUserRepo {
    pool: PgPool,
}

impl PostgresUserRepo {
    pub fn new(pool: PgPool) -> Self {
        Self { pool }
    }
}

fn err<E: std::fmt::Display>(e: E) -> RepoError {
    RepoError::Unknown(e.to_string())
}

#[async_trait]
impl UserRepo for PostgresUserRepo {
    async fn upsert(&self, params: UpsertParams) -> Result<User, RepoError> {
        let row: (Uuid, String) = sqlx::query_as(
            "INSERT INTO users (id, email) VALUES ($1, $2) \
             ON CONFLICT (id) DO UPDATE SET id = EXCLUDED.id \
             RETURNING id, email",
        )
        .bind(params.id)
        .bind(&params.email)
        .fetch_one(&self.pool)
        .await
        .map_err(err)?;

        Ok(User {
            id: row.0,
            email: row.1,
        })
    }

    async fn get(&self, id: Uuid) -> Result<User, RepoError> {
        let row: Option<(Uuid, String)> = sqlx::query_as("SELECT id, email FROM users WHERE id = $1")
            .bind(id)
            .fetch_optional(&self.pool)
            .await
            .map_err(err)?;
        let Some((id, email)) = row else {
            return Err(RepoError::NotFound);
        };
        Ok(User { id, email })
    }
}
