use crate::queue::sqs;
use crate::queue::sqs::SqsMessageQueue;
use aws_sdk_sqs::Client;
use itx_contract::queue::MessageQueue;
use itx_contract::queue::error::QueueError;
use itx_contract::queue::factory::MessageQueueFactory;
use std::sync::Arc;

pub struct SqsMessageQueueFactory {
    control_standard: Arc<dyn MessageQueue>,
    control_premium: Arc<dyn MessageQueue>,
    compute_standard: Arc<dyn MessageQueue>,
    compute_premium: Arc<dyn MessageQueue>,
}

impl SqsMessageQueueFactory {
    /// Construct from env. Resolves all four queue URLs eagerly via `GetQueueUrl` so misconfigured
    /// queues fail at startup, not on first use.
    ///
    /// Reads:
    ///   ITX_QUEUE_CONTROL_STANDARD (default "itx-control-standard")
    ///   ITX_QUEUE_CONTROL_PREMIUM  (default "itx-control-premium")
    ///   ITX_QUEUE_COMPUTE_STANDARD (default "itx-compute-standard")
    ///   ITX_QUEUE_COMPUTE_PREMIUM  (default "itx-compute-premium")
    pub async fn from_env() -> Result<Self, QueueError> {
        let client = sqs::client_from_env().await;

        let resolve = |client: Client, name: String| async move {
            let url = client
                .get_queue_url()
                .queue_name(&name)
                .send()
                .await
                .map_err(sqs::err)?
                .queue_url
                .ok_or_else(|| QueueError::Unknown(format!("missing queue url for {name}")))?;
            Ok::<Arc<dyn MessageQueue>, QueueError>(Arc::new(SqsMessageQueue::new(client, url)))
        };

        let control_standard = resolve(
            client.clone(),
            sqs::queue_name_env("ITX_QUEUE_CONTROL_STANDARD", "itx-control-standard"),
        )
        .await?;
        let control_premium = resolve(
            client.clone(),
            sqs::queue_name_env("ITX_QUEUE_CONTROL_PREMIUM", "itx-control-premium"),
        )
        .await?;
        let compute_standard = resolve(
            client.clone(),
            sqs::queue_name_env("ITX_QUEUE_COMPUTE_STANDARD", "itx-compute-standard"),
        )
        .await?;
        let compute_premium = resolve(
            client,
            sqs::queue_name_env("ITX_QUEUE_COMPUTE_PREMIUM", "itx-compute-premium"),
        )
        .await?;

        Ok(Self {
            control_standard,
            control_premium,
            compute_standard,
            compute_premium,
        })
    }
}

impl MessageQueueFactory for SqsMessageQueueFactory {
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
