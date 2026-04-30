pub mod feature;
pub mod middleware;
pub mod state;

use crate::state::AppState;
use axum::Router;
use axum::middleware::from_fn;
use tokio::signal;
use tracing::info;

pub fn create_app(app_state: AppState) -> Router {
    let public = Router::new().nest("/health", feature::health::create_router());

    Router::new()
        .nest("/api/v1", Router::new().merge(public))
        .layer(from_fn(middleware::wrap::wrap_response))
        .with_state(app_state)
}

pub async fn shutdown_signal() {
    let ctrl_c = async {
        signal::ctrl_c()
            .await
            .expect("failed to install Ctrl+C handler");
    };

    let terminate = async {
        signal::unix::signal(signal::unix::SignalKind::terminate())
            .expect("failed to install signal handler")
            .recv()
            .await;
    };

    tokio::select! {
        _ = ctrl_c => {
            info!("received Ctrl+C");
        },
        _ = terminate => {
            info!("received SIGTERM, shutting down...");
        },
    }
}
