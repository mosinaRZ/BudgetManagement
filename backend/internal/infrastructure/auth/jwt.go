package auth

import (
	"crypto/rand"
	"crypto/sha256"
	"encoding/base64"
	"errors"
	"fmt"
	"strings"
	"time"

	"github.com/golang-jwt/jwt/v5"
)

const (
	accessTokenType = "access"
	jwtIssuer       = "finance-sync-backend"
)

type accessClaims struct {
	TokenType string `json:"token_type"`
	jwt.RegisteredClaims
}

func GenerateAccessToken(userID string, ttl time.Duration, secret string) (string, error) {
	if strings.TrimSpace(userID) == "" || ttl <= 0 || strings.TrimSpace(secret) == "" {
		return "", errors.New("invalid access token parameters")
	}
	now := time.Now()
	claims := accessClaims{
		TokenType: accessTokenType,
		RegisteredClaims: jwt.RegisteredClaims{
			Issuer:    jwtIssuer,
			Subject:   userID,
			IssuedAt:  jwt.NewNumericDate(now),
			ExpiresAt: jwt.NewNumericDate(now.Add(ttl)),
		},
	}
	token := jwt.NewWithClaims(jwt.SigningMethodHS256, claims)
	token.Header["typ"] = "JWT"
	return token.SignedString([]byte(secret))
}

func GenerateRefreshToken() (string, error) {
	b := make([]byte, 32)
	if _, err := rand.Read(b); err != nil {
		return "", errors.New("failed to generate refresh token")
	}
	return base64.RawURLEncoding.EncodeToString(b), nil
}

func HashRefreshToken(token string) string {
	sum := sha256.Sum256([]byte(token))
	return fmt.Sprintf("%x", sum[:])
}

func ParseAndValidateAccessToken(tokenString, secret string) (string, error) {
	if strings.TrimSpace(tokenString) == "" || strings.TrimSpace(secret) == "" {
		return "", errors.New("invalid access token")
	}
	var claims accessClaims
	parser := jwt.NewParser(
		jwt.WithValidMethods([]string{jwt.SigningMethodHS256.Alg()}),
		jwt.WithIssuer(jwtIssuer),
		jwt.WithExpirationRequired(),
	)
	token, err := parser.ParseWithClaims(tokenString, &claims, func(token *jwt.Token) (interface{}, error) {
		if token.Method != jwt.SigningMethodHS256 {
			return nil, errors.New("unexpected signing method")
		}
		return []byte(secret), nil
	})
	if err != nil || token == nil || !token.Valid {
		return "", errors.New("invalid access token")
	}
	if claims.TokenType != accessTokenType || strings.TrimSpace(claims.Subject) == "" {
		return "", errors.New("invalid access token claims")
	}
	return claims.Subject, nil
}
