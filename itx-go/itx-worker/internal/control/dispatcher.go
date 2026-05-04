package control

import (
	"context"
	"encoding/json"
	"fmt"
	"log/slog"

	"github.com/chehsunliu/itx/itx-go/itx-contract/email"
	"github.com/chehsunliu/itx/itx-go/itx-contract/queue/message"
	"github.com/chehsunliu/itx/itx-go/itx-contract/repo/post"
)

type Dispatcher struct {
	state WorkerState
}

func NewDispatcher(state WorkerState) *Dispatcher {
	return &Dispatcher{state: state}
}

type envelope struct {
	Type string          `json:"type"`
	Raw  json.RawMessage `json:"-"`
}

func (d *Dispatcher) Handle(ctx context.Context, body string) error {
	var env struct {
		Type string `json:"type"`
	}
	if err := json.Unmarshal([]byte(body), &env); err != nil {
		return fmt.Errorf("invalid message: %w", err)
	}
	switch env.Type {
	case message.TypePostCreated:
		var msg message.PostCreatedMessage
		if err := json.Unmarshal([]byte(body), &msg); err != nil {
			return fmt.Errorf("invalid post.created: %w", err)
		}
		return d.handlePostCreated(ctx, msg)
	default:
		return fmt.Errorf("unknown message type: %s", env.Type)
	}
}

func (d *Dispatcher) handlePostCreated(ctx context.Context, msg message.PostCreatedMessage) error {
	p, err := d.state.PostRepo.Get(ctx, post.GetParams{ID: msg.PostID})
	if err != nil {
		return err
	}
	author, err := d.state.UserRepo.Get(ctx, msg.AuthorID)
	if err != nil {
		return err
	}
	subscribers, err := d.state.SubscriptionRepo.ListSubscribers(ctx, msg.AuthorID)
	if err != nil {
		return err
	}
	slog.Info(
		"sending post.created notifications",
		"post_id", msg.PostID,
		"author", author.Email,
		"subscribers", len(subscribers),
	)

	for _, sub := range subscribers {
		if err := d.state.EmailClient.Send(ctx, email.SendEmailMessage{
			To:      sub.Email,
			Subject: fmt.Sprintf("%s just published a new post", author.Email),
			Body:    fmt.Sprintf("Check out the new post: %s", p.Title),
		}); err != nil {
			return err
		}
	}
	return nil
}
