package notification

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"strings"
	"time"
)

// Config configures OTP delivery through the IPPanel Edge API
// (https://edge.ippanel.com/v1) using a pre-approved pattern.
type Config struct {
	// SMSWebhookURL is the full send endpoint, e.g. https://edge.ippanel.com/v1/api/send
	SMSWebhookURL string
	// SMSAuthToken is the IPPanel API token. It is sent as-is in the Authorization header.
	SMSAuthToken string
	// SMSFromNumber is the sender line in E.164 format, e.g. +983000505.
	SMSFromNumber string
	// SMSPatternCode is the approved pattern code. The pattern must define a variable named "code".
	SMSPatternCode string
	DevLog         bool
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

type patternRequest struct {
	SendingType string            `json:"sending_type"`
	FromNumber  string            `json:"from_number"`
	Code        string            `json:"code"`
	Recipients  []string          `json:"recipients"`
	Params      map[string]string `json:"params"`
}

type patternResponse struct {
	Meta struct {
		Status      bool   `json:"status"`
		Message     string `json:"message"`
		MessageCode string `json:"message_code"`
	} `json:"meta"`
}

func (p *Provider) sms(ctx context.Context, to, code string) error {
	if strings.TrimSpace(p.c.SMSWebhookURL) == "" || strings.TrimSpace(p.c.SMSFromNumber) == "" || strings.TrimSpace(p.c.SMSPatternCode) == "" {
		return fmt.Errorf("SMS provider is not configured")
	}
	body, err := json.Marshal(patternRequest{
		SendingType: "pattern",
		FromNumber:  p.c.SMSFromNumber,
		Code:        p.c.SMSPatternCode,
		Recipients:  []string{to},
		Params:      map[string]string{"code": code},
	})
	if err != nil {
		return err
	}
	req, err := http.NewRequestWithContext(ctx, http.MethodPost, p.c.SMSWebhookURL, bytes.NewReader(body))
	if err != nil {
		return err
	}
	req.Header.Set("Content-Type", "application/json")
	if p.c.SMSAuthToken != "" {
		req.Header.Set("Authorization", p.c.SMSAuthToken)
	}
	res, err := p.client.Do(req)
	if err != nil {
		return err
	}
	defer res.Body.Close()

	raw, _ := io.ReadAll(io.LimitReader(res.Body, 64<<10))
	var parsed patternResponse
	_ = json.Unmarshal(raw, &parsed)

	if res.StatusCode/100 != 2 {
		return fmt.Errorf("SMS provider returned %s (%s %s)", res.Status, parsed.Meta.MessageCode, parsed.Meta.Message)
	}
	// IPPanel reports business errors in meta.status, so a 2xx alone is not proof of delivery.
	if !parsed.Meta.Status {
		return fmt.Errorf("SMS provider rejected the request (%s %s)", parsed.Meta.MessageCode, parsed.Meta.Message)
	}
	return nil
}
