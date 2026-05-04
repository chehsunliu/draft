package email

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"os"
	"time"

	"github.com/chehsunliu/itx/itx-go/itx-contract/email"
)

type HttpEmailClient struct {
	url    string
	apiKey string
	client *http.Client
}

func FromEnv() *HttpEmailClient {
	return &HttpEmailClient{
		url:    os.Getenv("ITX_EMAIL_URL"),
		apiKey: os.Getenv("ITX_EMAIL_API_KEY"),
		client: &http.Client{Timeout: 30 * time.Second},
	}
}

func (c *HttpEmailClient) Send(ctx context.Context, msg email.SendEmailMessage) error {
	body, err := json.Marshal(msg)
	if err != nil {
		return err
	}
	req, err := http.NewRequestWithContext(ctx, http.MethodPost, c.url, bytes.NewReader(body))
	if err != nil {
		return err
	}
	req.Header.Set("Authorization", "Bearer "+c.apiKey)
	req.Header.Set("Content-Type", "application/json")
	resp, err := c.client.Do(req)
	if err != nil {
		return err
	}
	defer resp.Body.Close()
	if resp.StatusCode < 200 || resp.StatusCode >= 300 {
		respBody, _ := io.ReadAll(resp.Body)
		return fmt.Errorf("email API returned %d: %s", resp.StatusCode, string(respBody))
	}
	return nil
}
