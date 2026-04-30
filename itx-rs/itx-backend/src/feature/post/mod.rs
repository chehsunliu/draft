use crate::error::BackendError;
use crate::feature::post::use_case::list_posts;
use crate::middleware::context::ItxContext;
use crate::state::AppState;
use axum::routing::get;
use axum::{Extension, Json, Router};

pub mod use_case;

async fn list_posts(
    Extension(context): Extension<ItxContext>,
) -> Result<Json<list_posts::ExecuteOutput>, BackendError> {
    let params = list_posts::ExecuteParams {
        user_id: context.user_id.unwrap(),
    };
    let use_case = list_posts::ListPostsUseCase::new();
    Ok(Json(use_case.execute(params).await?))
}

pub fn create_router() -> Router<AppState> {
    Router::new().route("/", get(list_posts))
}
