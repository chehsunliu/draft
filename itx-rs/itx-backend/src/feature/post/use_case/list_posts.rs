use crate::error::BackendError;
use itx_contract::repo::post::PostRepo;
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
pub struct Item {}

pub struct ListPostsUseCase {
    post_repo: Arc<dyn PostRepo>,
}

impl ListPostsUseCase {
    pub fn new(post_repo: Arc<dyn PostRepo>) -> Self {
        Self { post_repo }
    }

    pub async fn execute(&self, params: ExecuteParams) -> Result<ExecuteOutput, BackendError> {
        Ok(ExecuteOutput { items: vec![] })
    }
}
