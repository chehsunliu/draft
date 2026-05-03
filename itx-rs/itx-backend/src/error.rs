use axum::Json;
use axum::http::StatusCode;
use axum::response::{IntoResponse, Response};

use itx_contract::repo::error::RepoError;

#[derive(Debug, thiserror::Error)]
pub enum BackendError {
    #[error("not found")]
    NotFound,
    #[error("{0}")]
    BadRequest(String),
    #[error("{0}")]
    Unknown(String),
}

impl From<RepoError> for BackendError {
    fn from(err: RepoError) -> Self {
        match err {
            RepoError::NotFound => BackendError::NotFound,
            RepoError::Unknown(s) => BackendError::Unknown(s),
        }
    }
}

impl IntoResponse for BackendError {
    fn into_response(self) -> Response {
        let (status, error_message) = match self {
            BackendError::NotFound => (StatusCode::NOT_FOUND, "not found".to_string()),
            BackendError::BadRequest(s) => (StatusCode::BAD_REQUEST, s),
            BackendError::Unknown(s) => (StatusCode::INTERNAL_SERVER_ERROR, s),
        };

        (status, Json(serde_json::json!({ "message": error_message }))).into_response()
    }
}
