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
    #[serde(default = "default_max_concurrency")]
    pub max_concurrency: u32,
    pub control_standard_queue: String,
    pub control_premium_queue: String,
    pub compute_standard_queue: String,
    pub compute_premium_queue: String,
}

fn default_max_concurrency() -> u32 {
    100
}

pub struct RabbitMessageQueueFactory {
    conn: Arc<Connection>,
    config: RabbitMessageQueueFactoryConfig,
}

impl RabbitMessageQueueFactory {
    pub async fn from_env() -> Result<Self, lapin::Error> {
        let config = envy::prefixed("ITX_RABBITMQ_")
            .from_env::<RabbitMessageQueueFactoryConfig>()
            .expect("failed to read RabbitMQ environment variables");

        let url = format!(
            "amqp://{}:{}@{}:{}/%2F",
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
            self.config.max_concurrency,
        ))
    }

    fn create_control_premium_queue(&self) -> Arc<dyn MessageQueue> {
        Arc::new(RabbitMessageQueue::new(
            self.conn.clone(),
            self.config.control_premium_queue.clone(),
            self.config.max_concurrency,
        ))
    }

    fn create_compute_standard_queue(&self) -> Arc<dyn MessageQueue> {
        Arc::new(RabbitMessageQueue::new(
            self.conn.clone(),
            self.config.compute_standard_queue.clone(),
            self.config.max_concurrency,
        ))
    }

    fn create_compute_premium_queue(&self) -> Arc<dyn MessageQueue> {
        Arc::new(RabbitMessageQueue::new(
            self.conn.clone(),
            self.config.compute_premium_queue.clone(),
            self.config.max_concurrency,
        ))
    }
}
