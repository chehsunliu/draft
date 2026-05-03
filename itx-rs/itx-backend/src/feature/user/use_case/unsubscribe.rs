use crate::error::BackendError;
use itx_contract::repo::subscription::{SubscriptionRepo, UnsubscribeParams};
use itx_contract::repo::user::UserRepo;
use std::sync::Arc;
use uuid::Uuid;

pub struct ExecuteParams {
    pub subscriber_id: Uuid,
    pub author_id: Uuid,
}

pub struct UnsubscribeUseCase {
    user_repo: Arc<dyn UserRepo>,
    subscription_repo: Arc<dyn SubscriptionRepo>,
}

impl UnsubscribeUseCase {
    pub fn new(user_repo: Arc<dyn UserRepo>, subscription_repo: Arc<dyn SubscriptionRepo>) -> Self {
        Self {
            user_repo,
            subscription_repo,
        }
    }

    pub async fn execute(&self, params: ExecuteParams) -> Result<(), BackendError> {
        if params.subscriber_id == params.author_id {
            return Err(BackendError::BadRequest("cannot unsubscribe from yourself".into()));
        }

        self.user_repo.get(params.author_id).await?;

        self.subscription_repo
            .unsubscribe(UnsubscribeParams {
                subscriber_id: params.subscriber_id,
                author_id: params.author_id,
            })
            .await?;
        Ok(())
    }
}
