pub mod error;
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

    // Future feature routers that need auth get nested here.
    let protected: Router<AppState> = Router::new()
        .nest("/posts", feature::post::create_router())
        .nest("/users", feature::user::create_users_router())
        .nest("/subscriptions", feature::user::create_subscriptions_router())
        .layer(from_fn(middleware::auth::require_user));

    Router::new()
        .nest("/api/v1", Router::new().merge(public).merge(protected))
        .layer(from_fn(middleware::context::extract_context))
        .layer(from_fn(middleware::wrap::wrap_response))
        .with_state(app_state)
}

pub async fn shutdown_signal() {
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
        _ = ctrl_c => {
            info!("received Ctrl+C");
        },
        _ = terminate => {
            info!("received SIGTERM, shutting down...");
        },
    }
}
