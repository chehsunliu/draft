use crate::error::BackendError;
use crate::feature::post::dto::PostDto;
use crate::feature::post::use_case::{create_post, delete_post, get_post, list_posts, update_post};
use crate::middleware::context::ItxContext;
use crate::state::AppState;
use axum::extract::{Path, Query, State};
use axum::http::StatusCode;
use axum::routing::{delete, get, patch, post};
use axum::{Extension, Json, Router};
use itx_contract::queue::MessageQueue;
use itx_contract::repo::post::{PostId, PostRepo};
use serde::Deserialize;
use std::sync::Arc;

pub mod dto;
pub mod use_case;

#[derive(Deserialize)]
struct ListPostsQuery {
    #[serde(default = "default_limit")]
    limit: u32,
    #[serde(default)]
    offset: u32,
}

fn default_limit() -> u32 {
    50
}

async fn list_posts(
    State(post_repo): State<Arc<dyn PostRepo>>,
    Extension(context): Extension<ItxContext>,
    Query(query): Query<ListPostsQuery>,
) -> Result<Json<list_posts::ExecuteOutput>, BackendError> {
    let use_case = list_posts::ListPostsUseCase::new(post_repo);
    let output = use_case
        .execute(list_posts::ExecuteParams {
            user_id: context.user_id.unwrap(),
            limit: query.limit,
            offset: query.offset,
        })
        .await?;
    Ok(Json(output))
}

async fn get_post(
    State(post_repo): State<Arc<dyn PostRepo>>,
    Extension(context): Extension<ItxContext>,
    Path(id): Path<PostId>,
) -> Result<Json<PostDto>, BackendError> {
    let use_case = get_post::GetPostUseCase::new(post_repo);
    let output = use_case
        .execute(get_post::ExecuteParams {
            id,
            user_id: context.user_id.unwrap(),
        })
        .await?;
    Ok(Json(output))
}

#[derive(Deserialize)]
struct CreatePostBody {
    title: String,
    body: String,
    #[serde(default)]
    tags: Vec<String>,
}

async fn create_post(
    State(post_repo): State<Arc<dyn PostRepo>>,
    State(control_standard_queue): State<Arc<dyn MessageQueue>>,
    Extension(context): Extension<ItxContext>,
    Json(body): Json<CreatePostBody>,
) -> Result<(StatusCode, Json<PostDto>), BackendError> {
    let use_case = create_post::CreatePostUseCase::new(post_repo, control_standard_queue);
    let output = use_case
        .execute(create_post::ExecuteParams {
            user_id: context.user_id.unwrap(),
            title: body.title,
            body: body.body,
            tags: body.tags,
        })
        .await?;
    Ok((StatusCode::CREATED, Json(output)))
}

#[derive(Deserialize)]
struct UpdatePostBody {
    title: Option<String>,
    body: Option<String>,
    tags: Option<Vec<String>>,
}

async fn update_post(
    State(post_repo): State<Arc<dyn PostRepo>>,
    Extension(context): Extension<ItxContext>,
    Path(id): Path<PostId>,
    Json(body): Json<UpdatePostBody>,
) -> Result<Json<PostDto>, BackendError> {
    let use_case = update_post::UpdatePostUseCase::new(post_repo);
    let output = use_case
        .execute(update_post::ExecuteParams {
            id,
            user_id: context.user_id.unwrap(),
            title: body.title,
            body: body.body,
            tags: body.tags,
        })
        .await?;
    Ok(Json(output))
}

async fn delete_post(
    State(post_repo): State<Arc<dyn PostRepo>>,
    Extension(context): Extension<ItxContext>,
    Path(id): Path<PostId>,
) -> Result<StatusCode, BackendError> {
    let use_case = delete_post::DeletePostUseCase::new(post_repo);
    use_case
        .execute(delete_post::ExecuteParams {
            id,
            user_id: context.user_id.unwrap(),
        })
        .await?;
    Ok(StatusCode::NO_CONTENT)
}

pub fn create_router() -> Router<AppState> {
    Router::new()
        .route("/", get(list_posts))
        .route("/", post(create_post))
        .route("/{id}", get(get_post))
        .route("/{id}", patch(update_post))
        .route("/{id}", delete(delete_post))
}
