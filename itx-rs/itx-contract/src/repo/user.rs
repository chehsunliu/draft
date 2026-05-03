use async_trait::async_trait;
use uuid::Uuid;

use crate::repo::error::RepoError;

#[derive(Debug, Clone)]
pub struct User {
    pub id: Uuid,
    pub email: String,
}

#[derive(Debug, Clone)]
pub struct UpsertParams {
    pub id: Uuid,
    pub email: String,
}

#[async_trait]
pub trait UserRepo: Send + Sync {
    /// Inserts the user if missing, then returns the current row.
    async fn upsert(&self, params: UpsertParams) -> Result<User, RepoError>;

    /// Returns `RepoError::NotFound` if no user with `id` exists.
    async fn get(&self, id: Uuid) -> Result<User, RepoError>;
}
