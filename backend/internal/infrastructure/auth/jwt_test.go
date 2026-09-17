package auth

import (
	"github.com/mosinaRZ/finance-sync-backend/internal/domain/entity"
	"strings"
	"testing"
	"time"

	"github.com/golang-jwt/jwt/v5"
)

const jwtTestSecret = "01234567890123456789012345678901"

func TestAccessTokenValid(t *testing.T) {
	tok, err := GenerateAccessToken("user-1", time.Minute, jwtTestSecret)
	if err != nil {
		t.Fatal(err)
	}
	id, err := ParseAndValidateAccessToken(tok, jwtTestSecret)
	if err != nil || id != "user-1" {
		t.Fatalf("id=%q err=%v", id, err)
	}
}
func TestAccessTokenExpired(t *testing.T) {
	now := time.Now()
	claims := accessClaims{
		TokenType: accessTokenType,
		RegisteredClaims: jwt.RegisteredClaims{
			Issuer:    jwtIssuer,
			Subject:   "user-1",
			IssuedAt:  jwt.NewNumericDate(now.Add(-2 * time.Minute)),
			ExpiresAt: jwt.NewNumericDate(now.Add(-time.Minute)),
		},
	}
	tok := jwt.NewWithClaims(jwt.SigningMethodHS256, claims)
	raw, err := tok.SignedString([]byte(jwtTestSecret))
	if err != nil {
		t.Fatal(err)
	}
	if _, err = ParseAndValidateAccessToken(raw, jwtTestSecret); err == nil {
		t.Fatal("expired token accepted")
	}
}
func TestAccessTokenWrongAlgorithm(t *testing.T) {
	claims := accessClaims{TokenType: accessTokenType, RegisteredClaims: jwt.RegisteredClaims{Issuer: jwtIssuer, Subject: "user-1", IssuedAt: jwt.NewNumericDate(time.Now()), ExpiresAt: jwt.NewNumericDate(time.Now().Add(time.Minute))}}
	tok := jwt.NewWithClaims(jwt.SigningMethodHS512, claims)
	raw, err := tok.SignedString([]byte(jwtTestSecret))
	if err != nil {
		t.Fatal(err)
	}
	if _, err = ParseAndValidateAccessToken(raw, jwtTestSecret); err == nil {
		t.Fatal("wrong algorithm accepted")
	}
}
func TestAccessTokenWrongType(t *testing.T) {
	claims := accessClaims{TokenType: "refresh", RegisteredClaims: jwt.RegisteredClaims{Issuer: jwtIssuer, Subject: "user-1", ExpiresAt: jwt.NewNumericDate(time.Now().Add(time.Minute))}}
	tok := jwt.NewWithClaims(jwt.SigningMethodHS256, claims)
	raw, _ := tok.SignedString([]byte(jwtTestSecret))
	if _, err := ParseAndValidateAccessToken(raw, jwtTestSecret); err == nil {
		t.Fatal("wrong token type accepted")
	}
}
func TestRefreshTokenHashDoesNotExposeToken(t *testing.T) {
	raw, err := GenerateRefreshToken()
	if err != nil {
		t.Fatal(err)
	}
	if strings.Contains(HashRefreshToken(raw), raw) {
		t.Fatal("hash contains raw token")
	}
}

func TestAccessTokenInvalidSignature(t *testing.T) {
	raw, err := GenerateAccessToken("user-1", time.Minute, jwtTestSecret)
	if err != nil {
		t.Fatal(err)
	}
	if _, err := ParseAndValidateAccessToken(raw, jwtTestSecret+"x"); err == nil {
		t.Fatal("invalid signature accepted")
	}
}

func TestAccessTokenCarriesRole(t *testing.T) {
	tok, err := GenerateAccessTokenWithRole("user-1", entity.RoleAdmin, time.Minute, jwtTestSecret)
	if err != nil {
		t.Fatal(err)
	}
	claims, err := ParseAndValidateAccessTokenClaims(tok, jwtTestSecret)
	if err != nil {
		t.Fatal(err)
	}
	if claims.UserID != "user-1" || claims.Role != entity.RoleAdmin {
		t.Fatalf("claims = %+v, want user-1/admin", claims)
	}
}
