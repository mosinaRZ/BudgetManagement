package notification

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"net/http"
	"net/smtp"
	"time"
)

type Config struct {
	SMSWebhookURL, SMSAuthToken, SMTPHost, SMTPPort, SMTPUser, SMTPPassword, EmailFrom string
	DevLog                                                                             bool
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
	if channel == "email" {
		return p.email(destination, code)
	}
	return p.sms(ctx, destination, code)
}
func (p *Provider) email(to, code string) error {
	if p.c.SMTPHost == "" || p.c.EmailFrom == "" {
		return fmt.Errorf("SMTP is not configured")
	}
	msg := []byte("To: " + to + "\r\nSubject: Cidna verification code\r\nContent-Type: text/plain; charset=UTF-8\r\n\r\nYour verification code is " + code + ". It expires soon.\r\n")
	addr := p.c.SMTPHost + ":" + p.c.SMTPPort
	var a smtp.Auth
	if p.c.SMTPUser != "" {
		a = smtp.PlainAuth("", p.c.SMTPUser, p.c.SMTPPassword, p.c.SMTPHost)
	}
	return smtp.SendMail(addr, a, p.c.EmailFrom, []string{to}, msg)
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
