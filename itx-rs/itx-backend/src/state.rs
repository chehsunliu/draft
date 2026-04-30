use itx_contract::repo::post::PostRepo;
use std::error::Error;
use std::sync::Arc;

#[derive(Clone)]
pub struct AppState {
    // pub post_repo: Arc<dyn PostRepo>,
}

impl AppState {
    pub async fn from_env() -> Result<Self, Box<dyn Error>> {
        Ok(Self {})
    }
}
