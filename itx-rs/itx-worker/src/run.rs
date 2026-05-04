use std::sync::Arc;
use std::time::Duration;

use futures_util::future::join_all;
use itx_contract::queue::{MessageHandler, MessageQueue};
use tokio::signal;
use tokio_util::sync::CancellationToken;

/// Run a worker that consumes from one or more queues, dispatching every message to `handler`.
/// Returns when SIGTERM/SIGINT is received and all queue listeners have finished (or the 30s
/// shutdown grace period expires).
pub async fn run(queues: Vec<Arc<dyn MessageQueue>>, handler: Arc<dyn MessageHandler>) {
    let token = CancellationToken::new();
    let mut handles = Vec::with_capacity(queues.len());

    for queue in queues {
        let handler = handler.clone();
        let token = token.clone();
        handles.push(tokio::spawn(async move {
            match queue.receive(handler, token).await {
                Ok(()) => tracing::info!("queue listener returned cleanly"),
                Err(e) => tracing::error!(error = %e, "queue listener errored"),
            }
        }));
    }

    shutdown_signal().await;
    tracing::info!("shutdown signal received; cancelling listeners");
    token.cancel();

    if tokio::time::timeout(Duration::from_secs(30), join_all(handles))
        .await
        .is_err()
    {
        tracing::warn!("some listeners did not finish within 30s");
    }
    tracing::info!("worker shutdown complete");
}

async fn shutdown_signal() {
    let ctrl_c = async {
        signal::ctrl_c().await.expect("failed to install Ctrl+C handler");
    };
    let terminate = async {
        signal::unix::signal(signal::unix::SignalKind::terminate())
            .expect("failed to install signal handler")
            .recv()
            .await;
    };
    tokio::select! {
        _ = ctrl_c => {},
        _ = terminate => {},
    }
}
