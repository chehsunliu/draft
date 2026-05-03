pub mod error;
pub mod factory;

use std::sync::Arc;

use async_trait::async_trait;
use error::QueueError;

/// Errors returned by a message handler. Anything implementing `std::error::Error` works.
pub type HandlerError = Box<dyn std::error::Error + Send + Sync>;

#[async_trait]
pub trait MessageHandler: Send + Sync {
    /// Process a single message. Returning `Ok` causes the queue to ack/delete the message;
    /// returning `Err` causes the queue to nack/reject so the broker can route to the DLQ.
    async fn handle(&self, body: &str) -> Result<(), HandlerError>;
}

#[async_trait]
pub trait MessageQueue: Send + Sync {
    /// Publish a message to this queue.
    async fn publish(&self, body: &str) -> Result<(), QueueError>;

    /// Run the consumer loop, dispatching each message to `handler`. Returns when the underlying
    /// connection closes or the future is cancelled.
    async fn receive(&self, handler: Arc<dyn MessageHandler>) -> Result<(), QueueError>;
}
