use axum::{
    body::Body,
    http::{Request, StatusCode},
    middleware::Next,
    response::{IntoResponse, Response},
};

use crate::middleware::context::ItxContext;

pub async fn require_user(req: Request<Body>, next: Next) -> Response {
    let has_user = req.extensions().get::<ItxContext>().and_then(|c| c.user_id).is_some();
    if !has_user {
        return (StatusCode::UNAUTHORIZED, "missing user").into_response();
    }
    next.run(req).await
}
