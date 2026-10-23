package notification

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"net/http"
	"time"
)

type Config struct {
	SMSWebhookURL, SMSAuthToken string
	DevLog                      bool
}
type Provider struct {
	c      Config
	client *http.Client
}

func New(c Config) *Provider { return &Provider{c: c, client: &http.Client{Timeout: 10 * time.Second}} }
func (p *Provider) Send(ctx context.Context, destination, channel, code string) error {
	if p.c.DevLog {
		fmt.Printf("OTP %s -> %s: %s\n", channel, destination, code)
		return nil
	}
	return p.sms(ctx, destination, code)
}
func (p *Provider) sms(ctx context.Context, to, code string) error {
	if p.c.SMSWebhookURL == "" {
		return fmt.Errorf("SMS provider is not configured")
	}
	body, _ := json.Marshal(map[string]string{"to": to, "code": code, "message": "Cidna verification code: " + code})
	req, err := http.NewRequestWithContext(ctx, http.MethodPost, p.c.SMSWebhookURL, bytes.NewReader(body))
	if err != nil {
		return err
	}
	req.Header.Set("Content-Type", "application/json")
	if p.c.SMSAuthToken != "" {
		req.Header.Set("Authorization", "Bearer "+p.c.SMSAuthToken)
	}
	res, err := p.client.Do(req)
	if err != nil {
		return err
	}
	defer res.Body.Close()
	if res.StatusCode/100 != 2 {
		return fmt.Errorf("SMS provider returned %s", res.Status)
	}
	return nil
}
