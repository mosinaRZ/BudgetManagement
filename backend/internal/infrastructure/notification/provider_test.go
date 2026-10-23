package notification

import (
	"context"
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"testing"
)

func newTestProvider(url string) *Provider {
	return New(Config{SMSWebhookURL: url, SMSAuthToken: "tok", SMSFromNumber: "+983000505", SMSPatternCode: "pat123"})
}

func TestSendBuildsIPPanelPatternRequest(t *testing.T) {
	var got patternRequest
	var auth string
	srv := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		auth = r.Header.Get("Authorization")
		_ = json.NewDecoder(r.Body).Decode(&got)
		_, _ = w.Write([]byte(`{"data":{"message_outbox_ids":[1]},"meta":{"status":true,"message":"ok","message_code":"200-1"}}`))
	}))
	defer srv.Close()

	if err := newTestProvider(srv.URL).Send(context.Background(), "+989120000000", "sms", "123456"); err != nil {
		t.Fatal(err)
	}
	if auth != "tok" {
		t.Fatalf("authorization header = %q", auth)
	}
	if got.SendingType != "pattern" || got.FromNumber != "+983000505" || got.Code != "pat123" ||
		len(got.Recipients) != 1 || got.Recipients[0] != "+989120000000" || got.Params["code"] != "123456" {
		t.Fatalf("unexpected body: %+v", got)
	}
}

func TestSendFailsWhenMetaStatusFalse(t *testing.T) {
	srv := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		_, _ = w.Write([]byte(`{"data":null,"meta":{"status":false,"message":"not found","message_code":"404-1"}}`))
	}))
	defer srv.Close()
	if err := newTestProvider(srv.URL).Send(context.Background(), "+989120000000", "sms", "123456"); err == nil {
		t.Fatal("expected error")
	}
}

func TestSendFailsOnHTTPError(t *testing.T) {
	srv := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) { w.WriteHeader(401) }))
	defer srv.Close()
	if err := newTestProvider(srv.URL).Send(context.Background(), "+989120000000", "sms", "123456"); err == nil {
		t.Fatal("expected error")
	}
}

func TestSendRequiresConfig(t *testing.T) {
	if err := New(Config{}).Send(context.Background(), "+989120000000", "sms", "123456"); err == nil {
		t.Fatal("expected error")
	}
}
