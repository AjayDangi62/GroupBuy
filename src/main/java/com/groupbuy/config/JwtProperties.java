package com.groupbuy.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for JWT authentication and token management.
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        String secret,
        long accessTokenExpirationMs,
        long refreshTokenExpirationMs,
        String issuer
) {
    public JwtProperties {
        if (secret == null || secret.isBlank()) {
            // Default 256-bit HMAC secret if not supplied
            secret = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
        }
        if (accessTokenExpirationMs <= 0) {
            accessTokenExpirationMs = 900_000L; // 15 mins
        }
        if (refreshTokenExpirationMs <= 0) {
            refreshTokenExpirationMs = 604_800_000L; // 7 days
        }
        if (issuer == null || issuer.isBlank()) {
            issuer = "groupbuy-api";
        }
    }
}
