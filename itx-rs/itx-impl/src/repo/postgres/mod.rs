use std::sync::Arc;

use itx_contract::repo::factory::RepoFactory;
use itx_contract::repo::post::PostRepo;
use itx_contract::repo::subscription::SubscriptionRepo;
use itx_contract::repo::user::UserRepo;
use sqlx::PgPool;
use sqlx::postgres::{PgConnectOptions, PgPoolOptions};

use crate::repo::postgres::post::PostgresPostRepo;
use crate::repo::postgres::subscription::PostgresSubscriptionRepo;
use crate::repo::postgres::user::PostgresUserRepo;

pub mod post;
pub mod subscription;
pub mod user;

#[derive(serde::Deserialize)]
struct PostgresRepoFactoryConfig {
    pub host: String,
    pub port: u16,
    pub db_name: String,
    pub user: String,
    pub password: String,
}

pub struct PostgresRepoFactory {
    pub pool: PgPool,
}

impl PostgresRepoFactory {
    pub async fn from_env() -> Result<Self, sqlx::Error> {
        let config = envy::prefixed("ITX_POSTGRES_")
            .from_env::<PostgresRepoFactoryConfig>()
            .expect("failed to read Postgres environment variables");

        let options = PgConnectOptions::new()
            .host(&config.host)
            .port(config.port)
            .database(&config.db_name)
            .username(&config.user)
            .password(&config.password);

        let pool = PgPoolOptions::new().max_connections(10).connect_with(options).await?;
        Ok(Self { pool })
    }
}

impl RepoFactory for PostgresRepoFactory {
    fn create_post_repo(&self) -> Arc<dyn PostRepo> {
        Arc::new(PostgresPostRepo::new(self.pool.clone()))
    }

    fn create_user_repo(&self) -> Arc<dyn UserRepo> {
        Arc::new(PostgresUserRepo::new(self.pool.clone()))
    }

    fn create_subscription_repo(&self) -> Arc<dyn SubscriptionRepo> {
        Arc::new(PostgresSubscriptionRepo::new(self.pool.clone()))
    }
}
