use std::error::Error;
use std::sync::Arc;

use itx_contract::queue::MessageQueue;
use itx_contract::queue::factory::MessageQueueFactory;
use itx_impl::queue::rabbitmq::factory::RabbitMessageQueueFactory;
use itx_impl::queue::sqs::factory::SqsMessageQueueFactory;

#[derive(Clone)]
pub struct ComputeWorkerState {
    pub control_standard_queue: Arc<dyn MessageQueue>,
    pub control_premium_queue: Arc<dyn MessageQueue>,
    pub compute_standard_queue: Arc<dyn MessageQueue>,
    pub compute_premium_queue: Arc<dyn MessageQueue>,
}

impl ComputeWorkerState {
    pub async fn from_env() -> Result<Self, Box<dyn Error>> {
        let queue_factory: Arc<dyn MessageQueueFactory> =
            match std::env::var("ITX_QUEUE_PROVIDER").as_deref().unwrap_or("sqs") {
                "sqs" => Arc::new(SqsMessageQueueFactory::from_env().await),
                "rabbitmq" => Arc::new(RabbitMessageQueueFactory::from_env().await?),
                other => panic!("unknown ITX_QUEUE_PROVIDER: {other}"),
            };

        Ok(Self {
            control_standard_queue: queue_factory.create_control_standard_queue(),
            control_premium_queue: queue_factory.create_control_premium_queue(),
            compute_standard_queue: queue_factory.create_compute_standard_queue(),
            compute_premium_queue: queue_factory.create_compute_premium_queue(),
        })
    }
}
