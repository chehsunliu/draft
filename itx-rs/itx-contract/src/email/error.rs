#[derive(Debug, thiserror::Error)]
pub enum EmailError {
    #[error("{0}")]
    Unknown(String),
}
