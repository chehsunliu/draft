use crate::error::BackendError;
use crate::feature::user::dto::UserDto;
use itx_contract::repo::user::{UpsertParams, UserRepo};
use std::sync::Arc;
use uuid::Uuid;

pub struct ExecuteParams {
    pub user_id: Uuid,
    pub email: String,
}

pub struct GetMeUseCase {
    user_repo: Arc<dyn UserRepo>,
}

impl GetMeUseCase {
    pub fn new(user_repo: Arc<dyn UserRepo>) -> Self {
        Self { user_repo }
    }

    pub async fn execute(&self, params: ExecuteParams) -> Result<UserDto, BackendError> {
        let user = self
            .user_repo
            .upsert(UpsertParams {
                id: params.user_id,
                email: params.email,
            })
            .await?;
        Ok(user.into())
    }
}
