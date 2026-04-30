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

#[derive(Debug, Clone)]
pub struct NewPost {
    pub author_id: AuthorId,
    pub title: String,
    pub body: String,
    pub tags: Vec<String>,
}

#[derive(Debug, Clone, Default)]
pub struct PostPatch {
    pub title: Option<String>,
    pub body: Option<String>,
    pub tags: Option<Vec<String>>,
}

#[derive(Debug, Clone, Default)]
pub struct ListPostsQuery {
    pub author_id: Option<AuthorId>,
    pub limit: u32,
    pub offset: u32,
}

#[async_trait]
pub trait PostRepo: Send + Sync {
    async fn list(&self, query: ListPostsQuery) -> Result<Vec<Post>, RepoError>;

    async fn get(&self, id: PostId) -> Result<Option<Post>, RepoError>;

    async fn create(&self, input: NewPost) -> Result<Post, RepoError>;

    /// Updates a post owned by `author_id`. Returns `None` if the post does
    /// not exist or is not owned by the caller — handlers should map both to 404.
    async fn update(
        &self,
        id: PostId,
        author_id: AuthorId,
        patch: PostPatch,
    ) -> Result<Option<Post>, RepoError>;

    /// Deletes a post owned by `author_id`. Returns `false` if the post does
    /// not exist or is not owned by the caller.
    async fn delete(&self, id: PostId, author_id: AuthorId) -> Result<bool, RepoError>;
}
