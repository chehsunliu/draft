use std::sync::Arc;

use crate::repo::post::PostRepo;
use crate::repo::user::UserRepo;

pub trait RepoFactory: Send + Sync {
    fn create_post_repo(&self) -> Arc<dyn PostRepo>;
    fn create_user_repo(&self) -> Arc<dyn UserRepo>;
}
