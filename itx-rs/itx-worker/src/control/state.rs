use std::error::Error;
use std::sync::Arc;

use itx_contract::queue::MessageQueue;
use itx_contract::queue::factory::MessageQueueFactory;
use itx_contract::repo::factory::RepoFactory;
use itx_contract::repo::post::PostRepo;
use itx_contract::repo::subscription::SubscriptionRepo;
use itx_contract::repo::user::UserRepo;
use itx_impl::queue::rabbitmq::factory::RabbitMessageQueueFactory;
use itx_impl::queue::sqs::factory::SqsMessageQueueFactory;
use itx_impl::repo::mariadb::factory::MariaDbRepoFactory;
use itx_impl::repo::postgres::factory::PostgresRepoFactory;
use serde::Deserialize;

#[derive(Clone, Deserialize)]
pub struct ControlWorkerStateProps {
    pub db_provider: Option<String>,
    pub queue_provider: Option<String>,
}

#[derive(Clone)]
pub struct ControlWorkerState {
    pub props: ControlWorkerStateProps,
    pub post_repo: Arc<dyn PostRepo>,
    pub user_repo: Arc<dyn UserRepo>,
    pub subscription_repo: Arc<dyn SubscriptionRepo>,
    pub control_standard_queue: Arc<dyn MessageQueue>,
    pub control_premium_queue: Arc<dyn MessageQueue>,
    pub compute_standard_queue: Arc<dyn MessageQueue>,
    pub compute_premium_queue: Arc<dyn MessageQueue>,
}

impl ControlWorkerState {
    pub async fn from_env() -> Result<Self, Box<dyn Error>> {
        let props = envy::prefixed("ITX_")
            .from_env::<ControlWorkerStateProps>()
            .expect("failed to read state props environment variables");
        let repo_factory: Arc<dyn RepoFactory> = match props.db_provider.as_deref().unwrap_or("postgres") {
            "postgres" => Arc::new(PostgresRepoFactory::from_env().await?),
            "mariadb" => Arc::new(MariaDbRepoFactory::from_env().await?),
            other => panic!("unknown ITX_DB_PROVIDER: {other}"),
        };
        let queue_factory: Arc<dyn MessageQueueFactory> = match props.queue_provider.as_deref().unwrap_or("sqs") {
            "sqs" => Arc::new(SqsMessageQueueFactory::from_env().await),
            "rabbitmq" => Arc::new(RabbitMessageQueueFactory::from_env().await?),
            other => panic!("unknown ITX_QUEUE_PROVIDER: {other}"),
        };

        Ok(Self {
            props,
            post_repo: repo_factory.create_post_repo(),
            user_repo: repo_factory.create_user_repo(),
            subscription_repo: repo_factory.create_subscription_repo(),
            control_standard_queue: queue_factory.create_control_standard_queue(),
            control_premium_queue: queue_factory.create_control_premium_queue(),
            compute_standard_queue: queue_factory.create_compute_standard_queue(),
            compute_premium_queue: queue_factory.create_compute_premium_queue(),
        })
    }
}
