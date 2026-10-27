package auth

import "encoding/base64"

func base64Raw(b []byte) string { return base64.RawStdEncoding.EncodeToString(b) }
