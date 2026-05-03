use std::sync::Arc;

use itx_contract::queue::MessageQueue;
use itx_contract::queue::factory::MessageQueueFactory;
use lapin::{Connection, ConnectionProperties};

use crate::queue::rabbitmq::RabbitMessageQueue;

#[derive(serde::Deserialize)]
struct RabbitMessageQueueFactoryConfig {
    pub host: String,
    pub port: u16,
    pub user: String,
    pub password: String,
    pub control_standard_queue: String,
    pub control_premium_queue: String,
    pub compute_standard_queue: String,
    pub compute_premium_queue: String,
}

pub struct RabbitMessageQueueFactory {
    pub conn: Arc<Connection>,
    config: RabbitMessageQueueFactoryConfig,
}

impl RabbitMessageQueueFactory {
    pub async fn from_env() -> Result<Self, lapin::Error> {
        let config = envy::prefixed("ITX_RABBITMQ_")
            .from_env::<RabbitMessageQueueFactoryConfig>()
            .expect("failed to read RabbitMQ environment variables");

        let url = format!(
            "amqp://{}:{}@{}:{}/",
            config.user, config.password, config.host, config.port
        );
        let conn = Connection::connect(&url, ConnectionProperties::default()).await?;

        Ok(Self {
            conn: Arc::new(conn),
            config,
        })
    }
}

impl MessageQueueFactory for RabbitMessageQueueFactory {
    fn create_control_standard_queue(&self) -> Arc<dyn MessageQueue> {
        Arc::new(RabbitMessageQueue::new(
            self.conn.clone(),
            self.config.control_standard_queue.clone(),
        ))
    }

    fn create_control_premium_queue(&self) -> Arc<dyn MessageQueue> {
        Arc::new(RabbitMessageQueue::new(
            self.conn.clone(),
            self.config.control_premium_queue.clone(),
        ))
    }

    fn create_compute_standard_queue(&self) -> Arc<dyn MessageQueue> {
        Arc::new(RabbitMessageQueue::new(
            self.conn.clone(),
            self.config.compute_standard_queue.clone(),
        ))
    }

    fn create_compute_premium_queue(&self) -> Arc<dyn MessageQueue> {
        Arc::new(RabbitMessageQueue::new(
            self.conn.clone(),
            self.config.compute_premium_queue.clone(),
        ))
    }
}
