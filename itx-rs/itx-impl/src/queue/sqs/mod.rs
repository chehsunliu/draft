pub mod factory;

use std::sync::Arc;

use async_trait::async_trait;
use aws_sdk_sqs::Client;
use itx_contract::queue::error::QueueError;
use itx_contract::queue::{MessageHandler, MessageQueue};

pub(crate) fn err<E: std::fmt::Display>(e: E) -> QueueError {
    QueueError::Unknown(e.to_string())
}

pub struct SqsMessageQueue {
    client: Client,
    queue_url: String,
}

impl SqsMessageQueue {
    pub fn new(client: Client, queue_url: impl Into<String>) -> Self {
        Self {
            client,
            queue_url: queue_url.into(),
        }
    }
}

#[async_trait]
impl MessageQueue for SqsMessageQueue {
    async fn publish(&self, body: &str) -> Result<(), QueueError> {
        self.client
            .send_message()
            .queue_url(&self.queue_url)
            .message_body(body)
            .send()
            .await
            .map_err(err)?;
        Ok(())
    }

    async fn receive(&self, handler: Arc<dyn MessageHandler>) -> Result<(), QueueError> {
        loop {
            let resp = self
                .client
                .receive_message()
                .queue_url(&self.queue_url)
                .max_number_of_messages(10)
                .wait_time_seconds(20)
                .send()
                .await
                .map_err(err)?;

            for msg in resp.messages.unwrap_or_default() {
                let Some(body) = msg.body else { continue };
                let Some(receipt) = msg.receipt_handle else { continue };

                match handler.handle(&body).await {
                    Ok(()) => {
                        // Success: delete the message so it doesn't reappear after the
                        // visibility timeout.
                        self.client
                            .delete_message()
                            .queue_url(&self.queue_url)
                            .receipt_handle(&receipt)
                            .send()
                            .await
                            .map_err(err)?;
                    }
                    Err(e) => {
                        // Failure: skip delete. The message becomes visible again after the
                        // visibility timeout, eventually landing in the DLQ once it exceeds
                        // maxReceiveCount.
                        tracing::warn!(error = %e, "handler failed; leaving message for retry/DLQ");
                    }
                }
            }
        }
    }
}
