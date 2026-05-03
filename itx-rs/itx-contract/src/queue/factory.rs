use std::sync::Arc;

use crate::queue::MessageQueue;

pub trait MessageQueueFactory: Send + Sync {
    fn create_control_standard_queue(&self) -> Arc<dyn MessageQueue>;
    fn create_control_premium_queue(&self) -> Arc<dyn MessageQueue>;
    fn create_compute_standard_queue(&self) -> Arc<dyn MessageQueue>;
    fn create_compute_premium_queue(&self) -> Arc<dyn MessageQueue>;
}
