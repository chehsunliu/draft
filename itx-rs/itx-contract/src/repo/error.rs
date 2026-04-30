#[derive(Debug, thiserror::Error)]
pub enum RepoError {
    #[error("not found")]
    NotFound,
    #[error("{0}")]
    Unknown(String),
}
