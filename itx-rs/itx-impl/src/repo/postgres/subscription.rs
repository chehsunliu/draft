use async_trait::async_trait;
use itx_contract::repo::error::RepoError;
use itx_contract::repo::subscription::{SubscribeParams, SubscriptionRepo, UnsubscribeParams};
use itx_contract::repo::user::User;
use sqlx::PgPool;
use uuid::Uuid;

pub struct PostgresSubscriptionRepo {
    pool: PgPool,
}

impl PostgresSubscriptionRepo {
    pub fn new(pool: PgPool) -> Self {
        Self { pool }
    }
}

fn err<E: std::fmt::Display>(e: E) -> RepoError {
    RepoError::Unknown(e.to_string())
}

#[async_trait]
impl SubscriptionRepo for PostgresSubscriptionRepo {
    async fn subscribe(&self, params: SubscribeParams) -> Result<(), RepoError> {
        sqlx::query(
            "INSERT INTO subscriptions (subscriber_id, author_id) VALUES ($1, $2) \
             ON CONFLICT (subscriber_id, author_id) DO NOTHING",
        )
        .bind(params.subscriber_id)
        .bind(params.author_id)
        .execute(&self.pool)
        .await
        .map_err(err)?;
        Ok(())
    }

    async fn unsubscribe(&self, params: UnsubscribeParams) -> Result<(), RepoError> {
        sqlx::query("DELETE FROM subscriptions WHERE subscriber_id = $1 AND author_id = $2")
            .bind(params.subscriber_id)
            .bind(params.author_id)
            .execute(&self.pool)
            .await
            .map_err(err)?;
        Ok(())
    }

    async fn list_authors(&self, subscriber_id: Uuid) -> Result<Vec<User>, RepoError> {
        let rows: Vec<(Uuid, String)> = sqlx::query_as(
            "SELECT u.id, u.email \
             FROM subscriptions s JOIN users u ON u.id = s.author_id \
             WHERE s.subscriber_id = $1 \
             ORDER BY s.created_at DESC, u.id ASC",
        )
        .bind(subscriber_id)
        .fetch_all(&self.pool)
        .await
        .map_err(err)?;

        Ok(rows.into_iter().map(|(id, email)| User { id, email }).collect())
    }
}
