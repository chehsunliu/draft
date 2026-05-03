use crate::error::BackendError;
use itx_contract::repo::subscription::{SubscribeParams, SubscriptionRepo};
use itx_contract::repo::user::{UpsertParams, UserRepo};
use std::sync::Arc;
use uuid::Uuid;

pub struct ExecuteParams {
    pub subscriber_id: Uuid,
    pub subscriber_email: String,
    pub author_id: Uuid,
}

pub struct SubscribeUseCase {
    user_repo: Arc<dyn UserRepo>,
    subscription_repo: Arc<dyn SubscriptionRepo>,
}

impl SubscribeUseCase {
    pub fn new(user_repo: Arc<dyn UserRepo>, subscription_repo: Arc<dyn SubscriptionRepo>) -> Self {
        Self {
            user_repo,
            subscription_repo,
        }
    }

    pub async fn execute(&self, params: ExecuteParams) -> Result<(), BackendError> {
        if params.subscriber_id == params.author_id {
            return Err(BackendError::BadRequest("cannot subscribe to yourself".into()));
        }

        // Pre-check the target so we return 404 cleanly instead of an FK violation.
        self.user_repo.get(params.author_id).await?;

        // Ensure the subscriber row exists; safe to call before /me has ever been hit.
        self.user_repo
            .upsert(UpsertParams {
                id: params.subscriber_id,
                email: params.subscriber_email,
            })
            .await?;

        self.subscription_repo
            .subscribe(SubscribeParams {
                subscriber_id: params.subscriber_id,
                author_id: params.author_id,
            })
            .await?;
        Ok(())
    }
}
