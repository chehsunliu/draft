pub mod error;

use async_trait::async_trait;
use serde::Serialize;

use crate::email::error::EmailError;

#[derive(Debug, Clone, Serialize)]
pub struct SendEmailMessage {
    pub to: String,
    pub subject: String,
    pub body: String,
}

#[async_trait]
pub trait EmailClient: Send + Sync {
    /// Send a single email. Returns `Ok(())` on a 2xx response from the upstream provider.
    async fn send(&self, msg: SendEmailMessage) -> Result<(), EmailError>;
}
