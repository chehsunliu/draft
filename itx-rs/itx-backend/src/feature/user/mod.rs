use crate::error::BackendError;
use crate::feature::user::dto::UserDto;
use crate::feature::user::use_case::{get_me, subscribe, unsubscribe};
use crate::middleware::context::ItxContext;
use crate::state::AppState;
use axum::extract::{Path, State};
use axum::http::StatusCode;
use axum::routing::{get, post};
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

async fn subscribe(
    State(user_repo): State<Arc<dyn UserRepo>>,
    State(subscription_repo): State<Arc<dyn SubscriptionRepo>>,
    Extension(context): Extension<ItxContext>,
    Path(author_id): Path<Uuid>,
) -> Result<StatusCode, BackendError> {
    let Some(email) = context.user_email.clone() else {
        return Err(BackendError::Unknown("missing X-Itx-User-Email".into()));
    };
    let use_case = subscribe::SubscribeUseCase::new(user_repo, subscription_repo);
    use_case
        .execute(subscribe::ExecuteParams {
            subscriber_id: context.user_id.unwrap(),
            subscriber_email: email,
            author_id,
        })
        .await?;
    Ok(StatusCode::NO_CONTENT)
}

async fn unsubscribe(
    State(user_repo): State<Arc<dyn UserRepo>>,
    State(subscription_repo): State<Arc<dyn SubscriptionRepo>>,
    Extension(context): Extension<ItxContext>,
    Path(author_id): Path<Uuid>,
) -> Result<StatusCode, BackendError> {
    let use_case = unsubscribe::UnsubscribeUseCase::new(user_repo, subscription_repo);
    use_case
        .execute(unsubscribe::ExecuteParams {
            subscriber_id: context.user_id.unwrap(),
            author_id,
        })
        .await?;
    Ok(StatusCode::NO_CONTENT)
}

pub fn create_router() -> Router<AppState> {
    Router::new()
        .route("/me", get(get_me))
        .route("/{id}/subscriptions", post(subscribe).delete(unsubscribe))
}
