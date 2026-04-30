use axum::{body::Body, http::Request, middleware::Next, response::Response};
use http_body_util::BodyExt;
use serde_json::json;

pub async fn wrap_response(req: Request<Body>, next: Next) -> Response {
    let response = next.run(req).await;

    let is_json = response
        .headers()
        .get(axum::http::header::CONTENT_TYPE)
        .and_then(|v| v.to_str().ok())
        .map(|v| v.starts_with("application/json"))
        .unwrap_or(false);

    let is_success = response.status().is_success();

    // case 2: success, non-json → pass through
    if is_success && !is_json {
        return response;
    }

    let (mut parts, body) = response.into_parts();

    let bytes = match body.collect().await {
        Ok(collected) => collected.to_bytes(),
        Err(_) => return Response::from_parts(parts, Body::empty()),
    };

    let wrapped = if is_json {
        // case 1: json body → wrap in _data/error
        let inner: serde_json::Value = match serde_json::from_slice(&bytes) {
            Ok(v) => v,
            Err(_) => return Response::from_parts(parts, Body::from(bytes)),
        };
        if is_success {
            json!({ "data": inner })
        } else {
            json!({ "error": inner })
        }
    } else {
        // case 3: error, no json → generic error message
        let detail = String::from_utf8_lossy(&bytes);
        let message = if detail.is_empty() {
            parts.status.canonical_reason().unwrap_or("unknown error").to_string()
        } else {
            detail.into_owned()
        };
        json!({ "error": { "message": message } })
    };

    let new_bytes = serde_json::to_vec(&wrapped).unwrap();

    parts.headers.insert(
        axum::http::header::CONTENT_TYPE,
        axum::http::HeaderValue::from_static("application/json"),
    );
    parts.headers.insert(
        axum::http::header::CONTENT_LENGTH,
        axum::http::HeaderValue::from(new_bytes.len()),
    );

    Response::from_parts(parts, Body::from(new_bytes))
}
