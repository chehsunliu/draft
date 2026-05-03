pub mod factory;

use std::sync::Arc;

use async_trait::async_trait;
use futures_util::StreamExt;
use itx_contract::queue::error::QueueError;
use itx_contract::queue::{MessageHandler, MessageQueue};
use lapin::options::{BasicAckOptions, BasicConsumeOptions, BasicPublishOptions, BasicQosOptions, BasicRejectOptions};
use lapin::types::FieldTable;
use lapin::{BasicProperties, Channel, Connection};
use tokio::sync::OnceCell;

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
}

impl RabbitMessageQueue {
    pub fn new(conn: Arc<Connection>, queue_name: impl Into<String>) -> Self {
        Self {
            conn,
            publish_channel: OnceCell::new(),
            consume_channel: OnceCell::new(),
            queue_name: queue_name.into(),
            consumer_tag: format!("itx-{}", uuid::Uuid::new_v4()),
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
                ch.basic_qos(10, BasicQosOptions::default()).await.map_err(err)?;
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

    async fn receive(&self, handler: Arc<dyn MessageHandler>) -> Result<(), QueueError> {
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

        while let Some(delivery) = consumer.next().await {
            let delivery = delivery.map_err(err)?;
            let body = match std::str::from_utf8(&delivery.data) {
                Ok(s) => s.to_string(),
                Err(e) => {
                    tracing::warn!(error = %e, "non-utf8 message body; rejecting to DLQ");
                    delivery
                        .reject(BasicRejectOptions { requeue: false })
                        .await
                        .map_err(err)?;
                    continue;
                }
            };

            match handler.handle(&body).await {
                Ok(()) => {
                    delivery.ack(BasicAckOptions::default()).await.map_err(err)?;
                }
                Err(e) => {
                    tracing::warn!(error = %e, "handler failed; rejecting to DLQ");
                    delivery
                        .reject(BasicRejectOptions { requeue: false })
                        .await
                        .map_err(err)?;
                }
            }
        }
        Ok(())
    }
}
