use async_trait::async_trait;
use itx_contract::repo::error::RepoError;
use itx_contract::repo::post::{
    AuthorId, ListPostsQuery, NewPost, Post, PostId, PostPatch, PostRepo,
};

pub struct PostgresPostRepo {}

#[async_trait]
impl PostRepo for PostgresPostRepo {
    async fn list(&self, _query: ListPostsQuery) -> Result<Vec<Post>, RepoError> {
        todo!()
    }

    async fn get(&self, _id: PostId) -> Result<Option<Post>, RepoError> {
        todo!()
    }

    async fn create(&self, _input: NewPost) -> Result<Post, RepoError> {
        todo!()
    }

    async fn update(
        &self,
        _id: PostId,
        _author_id: AuthorId,
        _patch: PostPatch,
    ) -> Result<Option<Post>, RepoError> {
        todo!()
    }

    async fn delete(&self, _id: PostId, _author_id: AuthorId) -> Result<bool, RepoError> {
        todo!()
    }
}
