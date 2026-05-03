use axum::{
    body::Body,
    http::{HeaderMap, Request, StatusCode},
    middleware::Next,
    response::{IntoResponse, Response},
};
use uuid::Uuid;

pub const HEADER_REQUEST_ID: &str = "X-Itx-Request-Id";
pub const HEADER_USER_ID: &str = "X-Itx-User-Id";
pub const HEADER_USER_EMAIL: &str = "X-Itx-User-Email";

#[derive(Clone, Debug)]
pub struct ItxContext {
    pub request_id: Uuid,
    pub user_id: Option<Uuid>,
    pub user_email: Option<String>,
}

fn parse_uuid_header(headers: &HeaderMap, name: &str) -> Result<Option<Uuid>, Response> {
    let Some(value) = headers.get(name) else {
        return Ok(None);
    };
    let s = value.to_str().map_err(|_| invalid_header(name))?;
    let id = Uuid::parse_str(s).map_err(|_| invalid_header(name))?;
    Ok(Some(id))
}

fn parse_string_header(headers: &HeaderMap, name: &str) -> Result<Option<String>, Response> {
    let Some(value) = headers.get(name) else {
        return Ok(None);
    };
    let s = value.to_str().map_err(|_| invalid_header(name))?;
    Ok(Some(s.to_string()))
}

fn invalid_header(name: &str) -> Response {
    (StatusCode::BAD_REQUEST, format!("invalid {name}")).into_response()
}

pub async fn extract_context(mut req: Request<Body>, next: Next) -> Response {
    let request_id = match parse_uuid_header(req.headers(), HEADER_REQUEST_ID) {
        Ok(Some(id)) => id,
        Ok(None) => Uuid::new_v4(),
        Err(resp) => return resp,
    };
    let user_id = match parse_uuid_header(req.headers(), HEADER_USER_ID) {
        Ok(v) => v,
        Err(resp) => return resp,
    };
    let user_email = match parse_string_header(req.headers(), HEADER_USER_EMAIL) {
        Ok(v) => v,
        Err(resp) => return resp,
    };

    req.extensions_mut().insert(ItxContext {
        request_id,
        user_id,
        user_email,
    });
    next.run(req).await
}
