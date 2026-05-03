use async_trait::async_trait;
use uuid::Uuid;

use crate::repo::error::RepoError;

#[derive(Debug, Clone)]
pub struct SubscribeParams {
    pub subscriber_id: Uuid,
    pub author_id: Uuid,
}

#[derive(Debug, Clone)]
pub struct UnsubscribeParams {
    pub subscriber_id: Uuid,
    pub author_id: Uuid,
}

#[async_trait]
pub trait SubscriptionRepo: Send + Sync {
    /// Inserts the subscription if missing. Idempotent.
    async fn subscribe(&self, params: SubscribeParams) -> Result<(), RepoError>;

    /// Removes the subscription if present. Idempotent.
    async fn unsubscribe(&self, params: UnsubscribeParams) -> Result<(), RepoError>;
}
