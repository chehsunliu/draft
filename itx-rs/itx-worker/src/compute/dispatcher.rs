use async_trait::async_trait;
use itx_contract::queue::{HandlerError, MessageHandler};

use crate::compute::state::ComputeWorkerState;

pub struct ComputeDispatcher {
    #[allow(dead_code)] // wired up by upcoming handlers (e.g. send-notification → email API)
    state: ComputeWorkerState,
}

impl ComputeDispatcher {
    pub fn new(state: ComputeWorkerState) -> Self {
        Self { state }
    }
}

#[async_trait]
impl MessageHandler for ComputeDispatcher {
    async fn handle(&self, body: &str) -> Result<(), HandlerError> {
        // No compute-plane message types yet — just log and ack so the queue stays drained.
        tracing::info!(body = body, "compute message received (no handler yet)");
        Ok(())
    }
}
