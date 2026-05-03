use async_trait::async_trait;
use itx_contract::repo::error::RepoError;
use itx_contract::repo::user::{UpsertParams, User, UserRepo};
use sqlx::MySqlPool;
use uuid::Uuid;

pub struct MariaDbUserRepo {
    pool: MySqlPool,
}

impl MariaDbUserRepo {
    pub fn new(pool: MySqlPool) -> Self {
        Self { pool }
    }
}

fn err<E: std::fmt::Display>(e: E) -> RepoError {
    RepoError::Unknown(e.to_string())
}

#[async_trait]
impl UserRepo for MariaDbUserRepo {
    async fn upsert(&self, params: UpsertParams) -> Result<User, RepoError> {
        sqlx::query("INSERT INTO users (id, email) VALUES (?, ?) ON DUPLICATE KEY UPDATE id = id")
            .bind(params.id.to_string())
            .bind(&params.email)
            .execute(&self.pool)
            .await
            .map_err(err)?;

        let row: (String, String) = sqlx::query_as("SELECT id, email FROM users WHERE id = ?")
            .bind(params.id.to_string())
            .fetch_one(&self.pool)
            .await
            .map_err(err)?;

        Ok(User {
            id: Uuid::parse_str(&row.0).map_err(err)?,
            email: row.1,
        })
    }

    async fn get(&self, id: Uuid) -> Result<User, RepoError> {
        let row: Option<(String, String)> = sqlx::query_as("SELECT id, email FROM users WHERE id = ?")
            .bind(id.to_string())
            .fetch_optional(&self.pool)
            .await
            .map_err(err)?;
        let Some((id_str, email)) = row else {
            return Err(RepoError::NotFound);
        };
        Ok(User {
            id: Uuid::parse_str(&id_str).map_err(err)?,
            email,
        })
    }
}
