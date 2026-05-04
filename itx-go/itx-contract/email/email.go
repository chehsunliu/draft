package email

import "context"

type SendEmailMessage struct {
	To      string `json:"to"`
	Subject string `json:"subject"`
	Body    string `json:"body"`
}

type EmailClient interface {
	Send(ctx context.Context, msg SendEmailMessage) error
}
