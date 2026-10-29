package com.groupbuy.auth.security;

import com.groupbuy.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

/**
 * Service for generating, signing, and parsing HMAC-SHA JWT access tokens using JJWT 0.12.x.
 * Enforces cryptographic signing and rejects unsigned or forged tokens (alg=none mitigation).
 */
@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    private final JwtProperties jwtProperties;
    private final SecretKey signingKey;

    public JwtTokenProvider(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.signingKey = initializeKey(jwtProperties.secret());
    }

    private SecretKey initializeKey(String secret) {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            // Pad or extend to 32 bytes (256-bit) if minimum length requirement is not met
            byte[] padded = new byte[32];
            System.arraycopy(keyBytes, 0, padded, 0, keyBytes.length);
            keyBytes = padded;
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Generates a signed JWT access token for the given authenticated principal.
     */
    public String generateAccessToken(UserPrincipal principal) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtProperties.accessTokenExpirationMs());

        List<String> roles = principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        return Jwts.builder()
                .subject(String.valueOf(principal.getId()))
                .claim("userId", principal.getId())
                .claim("email", principal.getEmail())
                .claim("username", principal.getUserHandle())
                .claim("roles", roles)
                .issuer(jwtProperties.issuer())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(signingKey)
                .compact();
    }

    /**
     * Validates signature and expiration of the JWT token string.
     * Returns true if valid, false if malformed, expired, or signature is invalid.
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (SecurityException ex) {
            log.warn("Invalid JWT signature: {}", ex.getMessage());
        } catch (MalformedJwtException ex) {
            log.warn("Malformed JWT token: {}", ex.getMessage());
        } catch (ExpiredJwtException ex) {
            log.warn("Expired JWT token: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            log.warn("Unsupported JWT token: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            log.warn("JWT claims string is empty or invalid: {}", ex.getMessage());
        } catch (JwtException ex) {
            log.warn("JWT validation failed: {}", ex.getMessage());
        }
        return false;
    }

    /**
     * Parses claims from a signed JWT token.
     */
    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Long getUserIdFromToken(String token) {
        Claims claims = parseClaims(token);
        Object userIdClaim = claims.get("userId");
        if (userIdClaim instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(claims.getSubject());
    }

    public String getEmailFromToken(String token) {
        Claims claims = parseClaims(token);
        return claims.get("email", String.class);
    }

    @SuppressWarnings("unchecked")
    public List<String> getRolesFromToken(String token) {
        Claims claims = parseClaims(token);
        return claims.get("roles", List.class);
    }

    public long getAccessTokenExpirationSeconds() {
        return jwtProperties.accessTokenExpirationMs() / 1000;
    }
}
