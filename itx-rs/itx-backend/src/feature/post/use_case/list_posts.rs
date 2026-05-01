use crate::error::BackendError;
use itx_contract::repo::error::RepoError;
use itx_contract::repo::post::{ListParams, Post, PostRepo};
use serde::Serialize;
use std::sync::Arc;
use uuid::Uuid;

pub struct ExecuteParams {
    pub user_id: Uuid,
}

#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub struct ExecuteOutput {
    pub items: Vec<Item>,
}

#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub struct Item {
    pub id: i64,
    pub author_id: Uuid,
    pub title: String,
    pub body: String,
    pub tags: Vec<String>,
    #[serde(with = "time::serde::rfc3339")]
    pub created_at: time::OffsetDateTime,
}

impl From<Post> for Item {
    fn from(post: Post) -> Self {
        Self {
            id: post.id,
            author_id: post.author_id,
            title: post.title,
            body: post.body,
            tags: post.tags,
            created_at: post.created_at,
        }
    }
}

pub struct ListPostsUseCase {
    post_repo: Arc<dyn PostRepo>,
}

impl ListPostsUseCase {
    pub fn new(post_repo: Arc<dyn PostRepo>) -> Self {
        Self { post_repo }
    }

    pub async fn execute(&self, params: ExecuteParams) -> Result<ExecuteOutput, BackendError> {
        let posts = self
            .post_repo
            .list(ListParams {
                author_id: Some(params.user_id),
                limit: 0,
                offset: 0,
            })
            .await
            .map_err(|e| match e {
                RepoError::NotFound => BackendError::Unknown("not found".into()),
                RepoError::Unknown(s) => BackendError::Unknown(s),
            })?;

        Ok(ExecuteOutput {
            items: posts.into_iter().map(Item::from).collect(),
        })
    }
}
