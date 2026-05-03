use crate::error::BackendError;
use crate::feature::user::dto::UserDto;
use crate::feature::user::use_case::get_me;
use crate::middleware::context::ItxContext;
use crate::state::AppState;
use axum::extract::State;
use axum::routing::get;
use axum::{Extension, Json, Router};
use itx_contract::repo::user::UserRepo;
use std::sync::Arc;

pub mod dto;
pub mod use_case;

async fn get_me(
    State(user_repo): State<Arc<dyn UserRepo>>,
    Extension(context): Extension<ItxContext>,
) -> Result<Json<UserDto>, BackendError> {
    let Some(email) = context.user_email.clone() else {
        return Err(BackendError::Unknown("missing X-Itx-User-Email".into()));
    };
    let use_case = get_me::GetMeUseCase::new(user_repo);
    let output = use_case
        .execute(get_me::ExecuteParams {
            user_id: context.user_id.unwrap(),
            email,
        })
        .await?;
    Ok(Json(output))
}

pub fn create_router() -> Router<AppState> {
    Router::new().route("/me", get(get_me))
}
