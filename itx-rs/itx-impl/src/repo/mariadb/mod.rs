use itx_contract::repo::factory::RepoFactory;
use sqlx::MySqlPool;
use sqlx::mysql::{MySqlConnectOptions, MySqlPoolOptions};

pub mod post;

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
            .password(&config.password);

        let pool = MySqlPoolOptions::new()
            .max_connections(10)
            .connect_with(options)
            .await?;
        Ok(Self { pool })
    }
}

impl RepoFactory for MariaDbRepoFactory {}
