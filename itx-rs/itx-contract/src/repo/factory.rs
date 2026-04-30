use std::sync::Arc;

use crate::repo::post::PostRepo;

pub trait RepoFactory: Send + Sync {
    fn create_post_repo(&self) -> Arc<dyn PostRepo>;
}
