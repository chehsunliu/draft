pub mod factory;

use std::sync::Arc;
use std::time::Duration;

use async_trait::async_trait;
use aws_config::BehaviorVersion;
use aws_sdk_sqs::Client;
use aws_sdk_sqs::config::{Credentials, Region};
use itx_contract::queue::error::QueueError;
use itx_contract::queue::factory::MessageQueueFactory;
use itx_contract::queue::{MessageHandler, MessageQueue};

fn err<E: std::fmt::Display>(e: E) -> QueueError {
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

/// Build an SQS client suitable for both real AWS and ElasticMQ. Reads:
///   ITX_SQS_ENDPOINT_URL  (optional — for ElasticMQ / LocalStack; if unset, uses real AWS)
///   ITX_SQS_REGION        (default "us-east-1")
///   ITX_SQS_ACCESS_KEY_ID and ITX_SQS_SECRET_ACCESS_KEY (defaulted to "x"/"x" for local dev)
pub async fn client_from_env() -> Client {
    let region = std::env::var("ITX_SQS_REGION").unwrap_or_else(|_| "us-east-1".to_string());
    let access_key = std::env::var("ITX_SQS_ACCESS_KEY_ID").unwrap_or_else(|_| "x".to_string());
    let secret_key = std::env::var("ITX_SQS_SECRET_ACCESS_KEY").unwrap_or_else(|_| "x".to_string());

    let mut loader = aws_config::defaults(BehaviorVersion::latest())
        .region(Region::new(region))
        .credentials_provider(Credentials::new(access_key, secret_key, None, None, "itx"))
        .timeout_config(
            aws_config::timeout::TimeoutConfig::builder()
                .operation_attempt_timeout(Duration::from_secs(30))
                .build(),
        );

    if let Ok(endpoint) = std::env::var("ITX_SQS_ENDPOINT_URL") {
        loader = loader.endpoint_url(endpoint);
    }

    Client::new(&loader.load().await)
}

fn queue_name_env(env: &str, default: &str) -> String {
    std::env::var(env).unwrap_or_else(|_| default.to_string())
}
