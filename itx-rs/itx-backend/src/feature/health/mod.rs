use crate::state::AppState;
use axum::routing::get;
use axum::{Json, Router};
use serde::Serialize;

#[derive(Serialize)]
struct ListTargetsResponse {
    status: String,
}

async fn list_targets() -> Json<ListTargetsResponse> {
    Json(ListTargetsResponse {
        status: "ok".to_string(),
    })
}

pub fn create_router() -> Router<AppState> {
    Router::new().route("/", get(list_targets))
}
