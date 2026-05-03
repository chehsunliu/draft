use async_trait::async_trait;
use itx_contract::repo::error::RepoError;
use itx_contract::repo::subscription::{SubscribeParams, SubscriptionRepo, UnsubscribeParams};
use sqlx::MySqlPool;

pub struct MariaDbSubscriptionRepo {
    pool: MySqlPool,
}

impl MariaDbSubscriptionRepo {
    pub fn new(pool: MySqlPool) -> Self {
        Self { pool }
    }
}

fn err<E: std::fmt::Display>(e: E) -> RepoError {
    RepoError::Unknown(e.to_string())
}

#[async_trait]
impl SubscriptionRepo for MariaDbSubscriptionRepo {
    async fn subscribe(&self, params: SubscribeParams) -> Result<(), RepoError> {
        sqlx::query("INSERT IGNORE INTO subscriptions (subscriber_id, author_id) VALUES (?, ?)")
            .bind(params.subscriber_id.to_string())
            .bind(params.author_id.to_string())
            .execute(&self.pool)
            .await
            .map_err(err)?;
        Ok(())
    }

    async fn unsubscribe(&self, params: UnsubscribeParams) -> Result<(), RepoError> {
        sqlx::query("DELETE FROM subscriptions WHERE subscriber_id = ? AND author_id = ?")
            .bind(params.subscriber_id.to_string())
            .bind(params.author_id.to_string())
            .execute(&self.pool)
            .await
            .map_err(err)?;
        Ok(())
    }
}
