#[derive(Debug, thiserror::Error)]
pub enum QueueError {
    #[error("{0}")]
    Unknown(String),
}
