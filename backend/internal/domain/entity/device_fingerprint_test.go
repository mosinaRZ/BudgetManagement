package entity

import "testing"

func TestNormalizeFingerprint(t *testing.T) {
	valid := "9F86D081884C7D659A2FEAA0C55AD015A3BF4F1B2B0B822CD15D6C15B0F00A08"
	cases := []struct {
		name, in, want string
	}{
		{"hex digest is lowercased", valid, "9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08"},
		{"surrounding spaces are trimmed", "  abcdef012345  ", "abcdef012345"},
		{"empty is dropped", "", ""},
		{"non-hex characters are dropped", "not-a-fingerprint", ""},
		{"persian digits are dropped", "۱۲۳۴", ""},
		{"over-long values are dropped", repeat("a", maxDeviceFingerprintLen+1), ""},
		{"maximum length is kept", repeat("f", maxDeviceFingerprintLen), repeat("f", maxDeviceFingerprintLen)},
	}
	for _, tc := range cases {
		t.Run(tc.name, func(t *testing.T) {
			if got := normalizeFingerprint(tc.in); got != tc.want {
				t.Fatalf("normalizeFingerprint(%q) = %q, want %q", tc.in, got, tc.want)
			}
		})
	}
}

func TestSetFingerprintStoresNormalizedValue(t *testing.T) {
	d := &Device{ID: "dev-1"}
	d.SetFingerprint("ABCDEF")
	if d.Fingerprint != "abcdef" {
		t.Fatalf("Fingerprint = %q, want %q", d.Fingerprint, "abcdef")
	}
	d.SetFingerprint("<script>")
	if d.Fingerprint != "" {
		t.Fatalf("invalid fingerprint should clear the value, got %q", d.Fingerprint)
	}
}

func repeat(s string, n int) string {
	out := make([]byte, 0, len(s)*n)
	for i := 0; i < n; i++ {
		out = append(out, s...)
	}
	return string(out)
}
