use crate::error::BackendError;
use crate::feature::post::dto::PostDto;
use itx_contract::queue::MessageQueue;
use itx_contract::queue::message::{MessageBody, PostCreatedMessageBody};
use itx_contract::repo::post::{CreateParams, PostRepo};
use std::sync::Arc;
use uuid::Uuid;

pub struct ExecuteParams {
    pub user_id: Uuid,
    pub title: String,
    pub body: String,
    pub tags: Vec<String>,
}

pub struct CreatePostUseCase {
    post_repo: Arc<dyn PostRepo>,
    control_standard_queue: Arc<dyn MessageQueue>,
}

impl CreatePostUseCase {
    pub fn new(post_repo: Arc<dyn PostRepo>, control_standard_queue: Arc<dyn MessageQueue>) -> Self {
        Self {
            post_repo,
            control_standard_queue,
        }
    }

    pub async fn execute(&self, params: ExecuteParams) -> Result<PostDto, BackendError> {
        let post = self
            .post_repo
            .create(CreateParams {
                author_id: params.user_id,
                title: params.title,
                body: params.body,
                tags: params.tags,
            })
            .await?;

        let body = serde_json::to_string(&MessageBody::PostCreated(PostCreatedMessageBody {
            post_id: post.id,
            author_id: post.author_id,
        }))
        .map_err(|e| BackendError::Unknown(e.to_string()))?;
        self.control_standard_queue.publish(&body).await?;

        Ok(post.into())
    }
}
