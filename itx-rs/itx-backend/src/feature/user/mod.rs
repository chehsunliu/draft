use crate::error::BackendError;
use crate::feature::user::dto::UserDto;
use crate::feature::user::use_case::{get_me, list_subscriptions};
use crate::middleware::context::ItxContext;
use crate::state::AppState;
use axum::extract::{Path, State};
use axum::routing::get;
use axum::{Extension, Json, Router};
use itx_contract::repo::subscription::SubscriptionRepo;
use itx_contract::repo::user::UserRepo;
use std::sync::Arc;
use uuid::Uuid;

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

async fn list_subscriptions(
    State(user_repo): State<Arc<dyn UserRepo>>,
    State(subscription_repo): State<Arc<dyn SubscriptionRepo>>,
    Path(id): Path<Uuid>,
) -> Result<Json<list_subscriptions::ExecuteOutput>, BackendError> {
    let use_case = list_subscriptions::ListSubscriptionsUseCase::new(user_repo, subscription_repo);
    let output = use_case
        .execute(list_subscriptions::ExecuteParams { subscriber_id: id })
        .await?;
    Ok(Json(output))
}

pub fn create_router() -> Router<AppState> {
    Router::new()
        .route("/me", get(get_me))
        .route("/{id}/subscriptions", get(list_subscriptions))
}
