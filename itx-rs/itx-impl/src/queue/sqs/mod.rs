pub mod factory;

use std::sync::Arc;

use async_trait::async_trait;
use aws_sdk_sqs::Client;
use itx_contract::queue::error::QueueError;
use itx_contract::queue::{MessageHandler, MessageQueue};
use tokio::sync::Semaphore;
use tokio::task::JoinSet;
use tokio_util::sync::CancellationToken;

pub(crate) fn err<E: std::fmt::Display>(e: E) -> QueueError {
    QueueError::Unknown(e.to_string())
}

pub struct SqsMessageQueue {
    client: Client,
    queue_url: String,
    semaphore: Arc<Semaphore>,
    max_concurrency: u32,
}

impl SqsMessageQueue {
    pub fn new(client: Client, queue_url: impl Into<String>, max_concurrency: u32) -> Self {
        Self {
            client,
            queue_url: queue_url.into(),
            semaphore: Arc::new(Semaphore::new(max_concurrency as usize)),
            max_concurrency,
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

    async fn receive(&self, handler: Arc<dyn MessageHandler>, cancel: CancellationToken) -> Result<(), QueueError> {
        // Cap each receive batch at SQS's max (10) and at our concurrency budget so we don't
        // pull messages we can't immediately dispatch.
        let batch = std::cmp::min(10, self.max_concurrency.max(1));
        let mut tasks: JoinSet<()> = JoinSet::new();

        loop {
            tokio::select! {
                _ = cancel.cancelled() => {
                    tracing::info!(queue = %self.queue_url, "cancellation received; stopping receive");
                    break;
                }
                resp = self
                    .client
                    .receive_message()
                    .queue_url(&self.queue_url)
                    .max_number_of_messages(batch as i32)
                    .wait_time_seconds(20)
                    .send() => {
                    let resp = resp.map_err(err)?;
                    for msg in resp.messages.unwrap_or_default() {
                        let Some(body) = msg.body else { continue };
                        let Some(receipt) = msg.receipt_handle else { continue };

                        let permit = self.semaphore.clone().acquire_owned().await.unwrap();
                        let handler = handler.clone();
                        let client = self.client.clone();
                        let queue_url = self.queue_url.clone();
                        tasks.spawn(async move {
                            let _permit = permit;
                            match handler.handle(&body).await {
                                Ok(()) => {
                                    if let Err(e) = client
                                        .delete_message()
                                        .queue_url(&queue_url)
                                        .receipt_handle(&receipt)
                                        .send()
                                        .await
                                    {
                                        tracing::error!(error = %e, "failed to delete message after success");
                                    }
                                }
                                Err(e) => {
                                    // Failure: skip delete. The message becomes visible again
                                    // after the visibility timeout, eventually landing in the
                                    // DLQ once it exceeds maxReceiveCount.
                                    tracing::warn!(error = %e, "handler failed; leaving message for retry/DLQ");
                                }
                            }
                        });
                    }
                }
            }
        }

        // Drain in-flight handler tasks before returning.
        tracing::info!(queue = %self.queue_url, in_flight = tasks.len(), "draining in-flight handlers");
        while tasks.join_next().await.is_some() {}
        Ok(())
    }
}
