#[derive(Debug, thiserror::Error)]
pub enum RepoError {
    #[error("{0}")]
    Unknown(String),
}
