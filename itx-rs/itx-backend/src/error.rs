use axum::Json;
use axum::http::StatusCode;
use axum::response::{IntoResponse, Response};

#[derive(Debug, thiserror::Error)]
pub enum BackendError {
    #[error("{0}")]
    Unknown(String),
}

impl IntoResponse for BackendError {
    fn into_response(self) -> Response {
        let (status, error_message) = match self {
            BackendError::Unknown(s) => (StatusCode::INTERNAL_SERVER_ERROR, s),
        };

        // Return a JSON response with the appropriate status code and message
        (status, Json(serde_json::json!({ "message": error_message }))).into_response()
    }
}
