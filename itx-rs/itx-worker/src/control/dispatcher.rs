use async_trait::async_trait;
use itx_contract::email::SendEmailMessage;
use itx_contract::queue::message::{MessageBody, PostCreatedMessageBody};
use itx_contract::queue::{HandlerError, MessageHandler};
use itx_contract::repo::post::GetParams;

use crate::control::state::ControlWorkerState;

pub struct ControlDispatcher {
    state: ControlWorkerState,
}

impl ControlDispatcher {
    pub fn new(state: ControlWorkerState) -> Self {
        Self { state }
    }

    async fn handle_post_created(&self, body: PostCreatedMessageBody) -> Result<(), HandlerError> {
        // Fetch the post (for the title) and the author (for the from-name).
        let post = self.state.post_repo.get(GetParams { id: body.post_id }).await?;
        let author = self.state.user_repo.get(body.author_id).await?;
        let subscribers = self.state.subscription_repo.list_subscribers(body.author_id).await?;
        tracing::info!(
            post_id = body.post_id,
            author = %author.email,
            subscribers = subscribers.len(),
            "sending post.created notifications"
        );

        for subscriber in subscribers {
            self.state
                .email_client
                .send(SendEmailMessage {
                    to: subscriber.email,
                    subject: format!("{} just published a new post", author.email),
                    body: format!("Check out the new post: {}", post.title),
                })
                .await?;
        }

        Ok(())
    }
}

#[async_trait]
impl MessageHandler for ControlDispatcher {
    async fn handle(&self, body: &str) -> Result<(), HandlerError> {
        let parsed: MessageBody = serde_json::from_str(body)?;
        match parsed {
            MessageBody::PostCreated(b) => self.handle_post_created(b).await,
        }
    }
}
