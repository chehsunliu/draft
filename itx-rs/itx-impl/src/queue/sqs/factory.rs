use std::sync::Arc;

use aws_sdk_sqs::Client;
use itx_contract::queue::MessageQueue;
use itx_contract::queue::factory::MessageQueueFactory;

use crate::queue::sqs::SqsMessageQueue;

#[derive(serde::Deserialize)]
struct SqsMessageQueueFactoryConfig {
    pub local_endpoint_url: Option<String>,
    pub control_standard_queue_url: String,
    pub control_premium_queue_url: String,
    pub compute_standard_queue_url: String,
    pub compute_premium_queue_url: String,
}

pub struct SqsMessageQueueFactory {
    pub client: Client,
    config: SqsMessageQueueFactoryConfig,
}

impl SqsMessageQueueFactory {
    pub async fn from_env() -> Self {
        let config = envy::prefixed("ITX_SQS_")
            .from_env::<SqsMessageQueueFactoryConfig>()
            .expect("failed to read SQS environment variables");

        let aws_config = aws_config::load_from_env().await;
        let mut sqs_config = aws_sdk_sqs::config::Builder::from(&aws_config);
        if let Some(endpoint) = &config.local_endpoint_url {
            sqs_config = sqs_config.endpoint_url(endpoint);
        }
        let client = Client::from_conf(sqs_config.build());

        Self { client, config }
    }
}

impl MessageQueueFactory for SqsMessageQueueFactory {
    fn create_control_standard_queue(&self) -> Arc<dyn MessageQueue> {
        Arc::new(SqsMessageQueue::new(
            self.client.clone(),
            self.config.control_standard_queue_url.clone(),
        ))
    }

    fn create_control_premium_queue(&self) -> Arc<dyn MessageQueue> {
        Arc::new(SqsMessageQueue::new(
            self.client.clone(),
            self.config.control_premium_queue_url.clone(),
        ))
    }

    fn create_compute_standard_queue(&self) -> Arc<dyn MessageQueue> {
        Arc::new(SqsMessageQueue::new(
            self.client.clone(),
            self.config.compute_standard_queue_url.clone(),
        ))
    }

    fn create_compute_premium_queue(&self) -> Arc<dyn MessageQueue> {
        Arc::new(SqsMessageQueue::new(
            self.client.clone(),
            self.config.compute_premium_queue_url.clone(),
        ))
    }
}
