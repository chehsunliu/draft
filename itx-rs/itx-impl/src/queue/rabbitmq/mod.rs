pub mod factory;

use std::sync::Arc;

use async_trait::async_trait;
use futures_util::StreamExt;
use itx_contract::queue::error::QueueError;
use itx_contract::queue::factory::MessageQueueFactory;
use itx_contract::queue::{MessageHandler, MessageQueue};
use lapin::options::{BasicAckOptions, BasicConsumeOptions, BasicPublishOptions, BasicQosOptions, BasicRejectOptions};
use lapin::types::FieldTable;
use lapin::{BasicProperties, Channel, Connection, ConnectionProperties};
use tokio::sync::Mutex;

fn err<E: std::fmt::Display>(e: E) -> QueueError {
    QueueError::Unknown(e.to_string())
}

pub struct RabbitMessageQueue {
    /// Channel used for publishing. Per RabbitMQ best practice we keep it separate from the
    /// consume channel — channels aren't safe for mixed concurrent reads/writes.
    publish_channel: Mutex<Channel>,
    /// Channel dedicated to consuming. Lives for the duration of `receive`.
    consume_channel: Mutex<Channel>,
    queue_name: String,
    consumer_tag: String,
}

impl RabbitMessageQueue {
    pub async fn new(conn: &Connection, queue_name: impl Into<String>) -> Result<Self, QueueError> {
        let queue_name = queue_name.into();
        let publish_channel = conn.create_channel().await.map_err(err)?;
        let consume_channel = conn.create_channel().await.map_err(err)?;
        consume_channel
            .basic_qos(10, BasicQosOptions::default())
            .await
            .map_err(err)?;

        Ok(Self {
            publish_channel: Mutex::new(publish_channel),
            consume_channel: Mutex::new(consume_channel),
            queue_name,
            consumer_tag: format!("itx-{}", uuid::Uuid::new_v4()),
        })
    }
}

#[async_trait]
impl MessageQueue for RabbitMessageQueue {
    async fn publish(&self, body: &str) -> Result<(), QueueError> {
        let channel = self.publish_channel.lock().await;
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
        let channel = self.consume_channel.lock().await;
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

/// Open a RabbitMQ connection using `ITX_RABBITMQ_URL` (e.g. `amqp://itx-admin:itx-admin@localhost:5672/`).
pub async fn connect_from_env() -> Result<Connection, QueueError> {
    let url = std::env::var("ITX_RABBITMQ_URL").map_err(|_| QueueError::Unknown("ITX_RABBITMQ_URL not set".into()))?;
    Connection::connect(&url, ConnectionProperties::default())
        .await
        .map_err(err)
}

fn queue_name_env(env: &str, default: &str) -> String {
    std::env::var(env).unwrap_or_else(|_| default.to_string())
}
