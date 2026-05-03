use crate::queue::rabbitmq;
use crate::queue::rabbitmq::RabbitMessageQueue;
use itx_contract::queue::MessageQueue;
use itx_contract::queue::error::QueueError;
use itx_contract::queue::factory::MessageQueueFactory;
use std::sync::Arc;

pub struct RabbitMessageQueueFactory {
    control_standard: Arc<dyn MessageQueue>,
    control_premium: Arc<dyn MessageQueue>,
    compute_standard: Arc<dyn MessageQueue>,
    compute_premium: Arc<dyn MessageQueue>,
}

impl RabbitMessageQueueFactory {
    /// Construct from env. Opens one connection and four queue channels eagerly.
    ///
    /// Reads:
    ///   ITX_RABBITMQ_URL
    ///   ITX_QUEUE_CONTROL_STANDARD (default "itx-control-standard")
    ///   ITX_QUEUE_CONTROL_PREMIUM  (default "itx-control-premium")
    ///   ITX_QUEUE_COMPUTE_STANDARD (default "itx-compute-standard")
    ///   ITX_QUEUE_COMPUTE_PREMIUM  (default "itx-compute-premium")
    pub async fn from_env() -> Result<Self, QueueError> {
        let conn = rabbitmq::connect_from_env().await?;
        let control_standard: Arc<dyn MessageQueue> = Arc::new(
            RabbitMessageQueue::new(
                &conn,
                rabbitmq::queue_name_env("ITX_QUEUE_CONTROL_STANDARD", "itx-control-standard"),
            )
            .await?,
        );
        let control_premium: Arc<dyn MessageQueue> = Arc::new(
            RabbitMessageQueue::new(
                &conn,
                rabbitmq::queue_name_env("ITX_QUEUE_CONTROL_PREMIUM", "itx-control-premium"),
            )
            .await?,
        );
        let compute_standard: Arc<dyn MessageQueue> = Arc::new(
            RabbitMessageQueue::new(
                &conn,
                rabbitmq::queue_name_env("ITX_QUEUE_COMPUTE_STANDARD", "itx-compute-standard"),
            )
            .await?,
        );
        let compute_premium: Arc<dyn MessageQueue> = Arc::new(
            RabbitMessageQueue::new(
                &conn,
                rabbitmq::queue_name_env("ITX_QUEUE_COMPUTE_PREMIUM", "itx-compute-premium"),
            )
            .await?,
        );

        Ok(Self {
            control_standard,
            control_premium,
            compute_standard,
            compute_premium,
        })
    }
}

impl MessageQueueFactory for RabbitMessageQueueFactory {
    fn create_control_standard_queue(&self) -> Arc<dyn MessageQueue> {
        self.control_standard.clone()
    }

    fn create_control_premium_queue(&self) -> Arc<dyn MessageQueue> {
        self.control_premium.clone()
    }

    fn create_compute_standard_queue(&self) -> Arc<dyn MessageQueue> {
        self.compute_standard.clone()
    }

    fn create_compute_premium_queue(&self) -> Arc<dyn MessageQueue> {
        self.compute_premium.clone()
    }
}
