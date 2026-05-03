use std::sync::Arc;

use itx_contract::repo::factory::RepoFactory;
use itx_contract::repo::post::PostRepo;
use itx_contract::repo::user::UserRepo;
use sqlx::MySqlPool;
use sqlx::mysql::{MySqlConnectOptions, MySqlPoolOptions, MySqlSslMode};

use crate::repo::mariadb::post::MariaDbPostRepo;
use crate::repo::mariadb::user::MariaDbUserRepo;

pub mod post;
pub mod user;

#[derive(serde::Deserialize)]
struct MariaDbRepoFactoryConfig {
    pub host: String,
    pub port: u16,
    pub db_name: String,
    pub user: String,
    pub password: String,
}

pub struct MariaDbRepoFactory {
    pub pool: MySqlPool,
}

impl MariaDbRepoFactory {
    pub async fn from_env() -> Result<Self, sqlx::Error> {
        let config = envy::prefixed("ITX_MARIADB_")
            .from_env::<MariaDbRepoFactoryConfig>()
            .expect("failed to read MariaDB environment variables");

        let options = MySqlConnectOptions::new()
            .host(&config.host)
            .port(config.port)
            .database(&config.db_name)
            .username(&config.user)
            .password(&config.password)
            .ssl_mode(MySqlSslMode::Disabled);

        let pool = MySqlPoolOptions::new()
            .max_connections(10)
            .connect_with(options)
            .await?;
        Ok(Self { pool })
    }
}

impl RepoFactory for MariaDbRepoFactory {
    fn create_post_repo(&self) -> Arc<dyn PostRepo> {
        Arc::new(MariaDbPostRepo::new(self.pool.clone()))
    }

    fn create_user_repo(&self) -> Arc<dyn UserRepo> {
        Arc::new(MariaDbUserRepo::new(self.pool.clone()))
    }
}
