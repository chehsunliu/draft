package message

import "github.com/google/uuid"

const TypePostCreated = "post.created"

type PostCreatedMessage struct {
	Type     string    `json:"type"`
	PostID   int64     `json:"postId"`
	AuthorID uuid.UUID `json:"authorId"`
}

func NewPostCreated(postID int64, authorID uuid.UUID) PostCreatedMessage {
	return PostCreatedMessage{Type: TypePostCreated, PostID: postID, AuthorID: authorID}
}
