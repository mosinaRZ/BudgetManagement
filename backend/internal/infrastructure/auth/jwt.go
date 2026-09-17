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

	"github.com/mosinaRZ/finance-sync-backend/internal/domain/entity"
)

const (
	accessTokenType = "access"
	jwtIssuer       = "finance-sync-backend"
)

type AccessTokenClaims struct {
	UserID string
	Role   entity.Role
}

type accessClaims struct {
	TokenType string      `json:"token_type"`
	Role      entity.Role `json:"role"`
	jwt.RegisteredClaims
}

func GenerateAccessToken(userID string, ttl time.Duration, secret string) (string, error) {
	return GenerateAccessTokenWithRole(userID, entity.RoleUser, ttl, secret)
}

func GenerateAccessTokenWithRole(userID string, role entity.Role, ttl time.Duration, secret string) (string, error) {
	if strings.TrimSpace(userID) == "" || !role.Valid() || ttl <= 0 || strings.TrimSpace(secret) == "" {
		return "", errors.New("invalid access token parameters")
	}
	now := time.Now()
	claims := accessClaims{
		TokenType: accessTokenType,
		Role:      role,
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
	claims, err := ParseAndValidateAccessTokenClaims(tokenString, secret)
	if err != nil {
		return "", err
	}
	return claims.UserID, nil
}

func ParseAndValidateAccessTokenClaims(tokenString, secret string) (AccessTokenClaims, error) {
	if strings.TrimSpace(tokenString) == "" || strings.TrimSpace(secret) == "" {
		return AccessTokenClaims{}, errors.New("invalid access token")
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
		return AccessTokenClaims{}, errors.New("invalid access token")
	}
	if claims.TokenType != accessTokenType || strings.TrimSpace(claims.Subject) == "" {
		return AccessTokenClaims{}, errors.New("invalid access token claims")
	}
	role := claims.Role
	if role == "" {
		role = entity.RoleUser
	}
	if !role.Valid() {
		return AccessTokenClaims{}, errors.New("invalid access token role")
	}
	return AccessTokenClaims{UserID: claims.Subject, Role: role}, nil
}
