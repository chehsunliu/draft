use crate::error::BackendError;
use serde::Serialize;
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

pub struct ListPostsUseCase {}

impl ListPostsUseCase {
    pub fn new() -> Self {
        Self {}
    }

    pub async fn execute(&self, params: ExecuteParams) -> Result<ExecuteOutput, BackendError> {
        Ok(ExecuteOutput { items: vec![] })
    }
}
