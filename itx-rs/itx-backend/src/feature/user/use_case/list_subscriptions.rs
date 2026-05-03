use crate::error::BackendError;
use crate::feature::user::dto::UserDto;
use itx_contract::repo::subscription::SubscriptionRepo;
use itx_contract::repo::user::UserRepo;
use serde::Serialize;
use std::sync::Arc;
use uuid::Uuid;

pub struct ExecuteParams {
    pub subscriber_id: Uuid,
}

#[derive(Serialize)]
#[serde(rename_all = "camelCase")]
pub struct ExecuteOutput {
    pub items: Vec<UserDto>,
}

pub struct ListSubscriptionsUseCase {
    user_repo: Arc<dyn UserRepo>,
    subscription_repo: Arc<dyn SubscriptionRepo>,
}

impl ListSubscriptionsUseCase {
    pub fn new(user_repo: Arc<dyn UserRepo>, subscription_repo: Arc<dyn SubscriptionRepo>) -> Self {
        Self {
            user_repo,
            subscription_repo,
        }
    }

    pub async fn execute(&self, params: ExecuteParams) -> Result<ExecuteOutput, BackendError> {
        // Pre-check the subject so an unknown user yields 404, not an empty list.
        self.user_repo.get(params.subscriber_id).await?;

        let authors = self.subscription_repo.list_authors(params.subscriber_id).await?;
        Ok(ExecuteOutput {
            items: authors.into_iter().map(UserDto::from).collect(),
        })
    }
}
