use serde::{Deserialize, Serialize};
use uuid::Uuid;

#[derive(Debug, Serialize, Deserialize)]
#[serde(tag = "type")]
pub enum MessageBody {
    #[serde(rename = "post.created")]
    PostCreated(PostCreatedMessageBody),
}

#[derive(Debug, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct PostCreatedMessageBody {
    pub post_id: i64,
    pub author_id: Uuid,
}
