pub mod factory;

use std::sync::Arc;

use async_trait::async_trait;
use futures_util::StreamExt;
use itx_contract::queue::error::QueueError;
use itx_contract::queue::{MessageHandler, MessageQueue};
use lapin::options::{BasicAckOptions, BasicConsumeOptions, BasicPublishOptions, BasicQosOptions, BasicRejectOptions};
use lapin::types::FieldTable;
use lapin::{BasicProperties, Channel, Connection};
use tokio::sync::{OnceCell, Semaphore};
use tokio::task::JoinSet;
use tokio_util::sync::CancellationToken;

pub(crate) fn err<E: std::fmt::Display>(e: E) -> QueueError {
    QueueError::Unknown(e.to_string())
}

pub struct RabbitMessageQueue {
    conn: Arc<Connection>,
    /// Lazily-initialized publish channel. Created on the first `publish` call. Per RabbitMQ
    /// best practice it's separate from the consume channel — channels aren't safe for mixed
    /// concurrent reads and writes.
    publish_channel: OnceCell<Channel>,
    /// Lazily-initialized consume channel. Created on the first `receive` call.
    consume_channel: OnceCell<Channel>,
    queue_name: String,
    consumer_tag: String,
    semaphore: Arc<Semaphore>,
    max_concurrency: u32,
}

impl RabbitMessageQueue {
    pub fn new(conn: Arc<Connection>, queue_name: impl Into<String>, max_concurrency: u32) -> Self {
        Self {
            conn,
            publish_channel: OnceCell::new(),
            consume_channel: OnceCell::new(),
            queue_name: queue_name.into(),
            consumer_tag: format!("itx-{}", uuid::Uuid::new_v4()),
            semaphore: Arc::new(Semaphore::new(max_concurrency as usize)),
            max_concurrency,
        }
    }

    async fn publish_chan(&self) -> Result<&Channel, QueueError> {
        self.publish_channel
            .get_or_try_init(|| async { self.conn.create_channel().await.map_err(err) })
            .await
    }

    async fn consume_chan(&self) -> Result<&Channel, QueueError> {
        self.consume_channel
            .get_or_try_init(|| async {
                let ch = self.conn.create_channel().await.map_err(err)?;
                // Match prefetch to max_concurrency so the broker doesn't dispatch faster than
                // we can hold semaphore permits. u16 cap because basic_qos prefetch is u16.
                let prefetch = std::cmp::min(self.max_concurrency, u16::MAX as u32) as u16;
                ch.basic_qos(prefetch, BasicQosOptions::default()).await.map_err(err)?;
                Ok(ch)
            })
            .await
    }
}

#[async_trait]
impl MessageQueue for RabbitMessageQueue {
    async fn publish(&self, body: &str) -> Result<(), QueueError> {
        let channel = self.publish_chan().await?;
        channel
            .basic_publish(
                "", // default exchange — routing_key acts as queue name
                &self.queue_name,
                BasicPublishOptions::default(),
                body.as_bytes(),
                BasicProperties::default().with_delivery_mode(2), // persistent
            )
            .await
            .map_err(err)?
            .await
            .map_err(err)?;
        Ok(())
    }

    async fn receive(&self, handler: Arc<dyn MessageHandler>, cancel: CancellationToken) -> Result<(), QueueError> {
        let channel = self.consume_chan().await?;
        let mut consumer = channel
            .basic_consume(
                &self.queue_name,
                &self.consumer_tag,
                BasicConsumeOptions::default(),
                FieldTable::default(),
            )
            .await
            .map_err(err)?;

        let mut tasks: JoinSet<()> = JoinSet::new();

        loop {
            tokio::select! {
                _ = cancel.cancelled() => {
                    tracing::info!(queue = %self.queue_name, "cancellation received; stopping receive");
                    break;
                }
                next = consumer.next() => {
                    let Some(delivery) = next else { break };
                    let delivery = delivery.map_err(err)?;

                    let permit = self.semaphore.clone().acquire_owned().await.unwrap();
                    let handler = handler.clone();
                    tasks.spawn(async move {
                        let _permit = permit;
                        let body = match std::str::from_utf8(&delivery.data) {
                            Ok(s) => s.to_string(),
                            Err(e) => {
                                tracing::warn!(error = %e, "non-utf8 message body; rejecting to DLQ");
                                if let Err(e) = delivery.reject(BasicRejectOptions { requeue: false }).await {
                                    tracing::error!(error = %e, "failed to reject non-utf8 message");
                                }
                                return;
                            }
                        };

                        match handler.handle(&body).await {
                            Ok(()) => {
                                if let Err(e) = delivery.ack(BasicAckOptions::default()).await {
                                    tracing::error!(error = %e, "failed to ack message after success");
                                }
                            }
                            Err(e) => {
                                tracing::warn!(error = %e, "handler failed; rejecting to DLQ");
                                if let Err(e) = delivery.reject(BasicRejectOptions { requeue: false }).await {
                                    tracing::error!(error = %e, "failed to reject message after handler failure");
                                }
                            }
                        }
                    });
                }
            }
        }

        tracing::info!(queue = %self.queue_name, in_flight = tasks.len(), "draining in-flight handlers");
        while tasks.join_next().await.is_some() {}
        Ok(())
    }
}
