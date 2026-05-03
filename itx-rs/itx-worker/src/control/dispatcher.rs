use async_trait::async_trait;
use itx_contract::queue::message::{MessageBody, PostCreatedMessageBody};
use itx_contract::queue::{HandlerError, MessageHandler};

use crate::control::state::ControlWorkerState;

pub struct ControlDispatcher {
    #[allow(dead_code)] // wired up by upcoming handlers (subscriber lookup, etc.)
    state: ControlWorkerState,
}

impl ControlDispatcher {
    pub fn new(state: ControlWorkerState) -> Self {
        Self { state }
    }

    async fn handle_post_created(&self, body: PostCreatedMessageBody) -> Result<(), HandlerError> {
        // TODO: fan out to subscribers, publish per-recipient send tasks to compute queue.
        tracing::info!(post_id = body.post_id, author_id = %body.author_id, "post.created received");
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
