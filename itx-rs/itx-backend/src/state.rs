use axum::extract::FromRef;
use itx_contract::repo::factory::RepoFactory;
use itx_contract::repo::post::PostRepo;
use itx_impl::repo::mariadb::MariaDbRepoFactory;
use itx_impl::repo::postgres::PostgresRepoFactory;
use std::error::Error;
use std::sync::Arc;

#[derive(Clone)]
pub struct AppState {
    pub post_repo: Arc<dyn PostRepo>,
}

impl AppState {
    pub async fn from_env() -> Result<Self, Box<dyn Error>> {
        let repo_factory: Arc<dyn RepoFactory> = match std::env::var("ITX_DB_PROVIDER").as_deref().unwrap_or("postgres")
        {
            "postgres" => Arc::new(PostgresRepoFactory::from_env().await?),
            "mariadb" => Arc::new(MariaDbRepoFactory::from_env().await?),
            other => panic!("unknown ITX_DB_PROVIDER: {other}"),
        };
        let post_repo = repo_factory.create_post_repo();

        Ok(Self { post_repo })
    }
}

impl FromRef<AppState> for Arc<dyn PostRepo> {
    fn from_ref(app_state: &AppState) -> Self {
        app_state.post_repo.clone()
    }
}
