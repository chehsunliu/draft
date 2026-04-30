use crate::repo::error::RepoError;
use async_trait::async_trait;
use uuid::Uuid;

pub type PostId = i64;
pub type AuthorId = Uuid;

#[derive(Debug, Clone)]
pub struct Post {
    pub id: PostId,
    pub author_id: AuthorId,
    pub title: String,
    pub body: String,
    pub tags: Vec<String>,
    pub created_at: time::OffsetDateTime,
}

#[derive(Debug, Clone, Default)]
pub struct ListParams {
    pub author_id: Option<AuthorId>,
    pub limit: u32,
    pub offset: u32,
}

#[derive(Debug, Clone)]
pub struct GetParams {
    pub id: PostId,
}

#[derive(Debug, Clone)]
pub struct CreateParams {
    pub author_id: AuthorId,
    pub title: String,
    pub body: String,
    pub tags: Vec<String>,
}

#[derive(Debug, Clone)]
pub struct UpdateParams {
    pub id: PostId,
    pub author_id: AuthorId,
    pub title: Option<String>,
    pub body: Option<String>,
    pub tags: Option<Vec<String>>,
}

#[derive(Debug, Clone)]
pub struct DeleteParams {
    pub id: PostId,
    pub author_id: AuthorId,
}

#[async_trait]
pub trait PostRepo: Send + Sync {
    async fn list(&self, params: ListParams) -> Result<Vec<Post>, RepoError>;

    /// Returns `RepoError::NotFound` if no post with `params.id` exists.
    async fn get(&self, params: GetParams) -> Result<Post, RepoError>;

    async fn create(&self, params: CreateParams) -> Result<Post, RepoError>;

    /// Updates a post owned by `params.author_id`. Returns `RepoError::NotFound`
    /// if the post does not exist or is not owned by the caller.
    async fn update(&self, params: UpdateParams) -> Result<Post, RepoError>;

    /// Deletes a post owned by `params.author_id`. Returns `RepoError::NotFound`
    /// if the post does not exist or is not owned by the caller.
    async fn delete(&self, params: DeleteParams) -> Result<(), RepoError>;
}
