package com.groupbuy.auth.service;

import com.groupbuy.auth.entity.RefreshToken;
import com.groupbuy.auth.repository.RefreshTokenRepository;
import com.groupbuy.common.exception.TokenRefreshException;
import com.groupbuy.config.JwtProperties;
import com.groupbuy.user.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Service managing opaque refresh tokens with rotation and reuse detection.
 *
 * <p>Tokens are never stored in plaintext in the database; only a SHA-256 hash
 * is persisted. On every token exchange, the old token is invalidated and a successor
 * is generated within the same rotation family. If an already revoked token is re-submitted,
 * the entire token family is immediately revoked to mitigate stolen credential attacks.
 */
@Service
public class RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProperties jwtProperties;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository, JwtProperties jwtProperties) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtProperties = jwtProperties;
    }

    /**
     * Issues a brand new refresh token and starts a new rotation family for a fresh login event.
     */
    @Transactional
    public TokenPair createRefreshToken(User user) {
        String rawToken = generateRawTokenValue();
        String tokenHash = hashToken(rawToken);
        String familyId = UUID.randomUUID().toString();
        Instant expiresAt = Instant.now().plusMillis(jwtProperties.refreshTokenExpirationMs());

        RefreshToken refreshToken = new RefreshToken(user, tokenHash, expiresAt, familyId);
        refreshTokenRepository.save(refreshToken);

        return new TokenPair(rawToken, refreshToken);
    }

    /**
     * Rotates a refresh token: invalidates the presented token and creates a replacement in the same family.
     * Enforces reuse detection.
     */
    @Transactional(noRollbackFor = TokenRefreshException.class)
    public TokenRotationResult rotateRefreshToken(String rawRefreshToken) {
        String tokenHash = hashToken(rawRefreshToken);
        RefreshToken token = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new TokenRefreshException("Refresh token was not recognized", "INVALID_REFRESH_TOKEN"));

        // Reuse detection check: if token was already revoked, a replay or breach has occurred
        if (token.isRevoked()) {
            log.warn("Refresh token reuse detected for familyId: {} and userId: {}. Revoking entire family.",
                    token.getFamilyId(), token.getUser().getId());
            refreshTokenRepository.revokeAllByFamilyId(token.getFamilyId());
            throw new TokenRefreshException(
                    "Refresh token reuse detected. All tokens in this session family have been invalidated for security.",
                    "TOKEN_REUSE_DETECTED"
            );
        }

        // Expiration check
        if (token.isExpired()) {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
            log.warn("Expired refresh token presented for userId: {}", token.getUser().getId());
            throw new TokenRefreshException("Refresh token has expired. Please log in again.", "REFRESH_TOKEN_EXPIRED");
        }

        // Invalidate current token
        token.setRevoked(true);

        // Issue new successor token in the same family
        String newRawToken = generateRawTokenValue();
        String newTokenHash = hashToken(newRawToken);
        Instant newExpiresAt = Instant.now().plusMillis(jwtProperties.refreshTokenExpirationMs());

        RefreshToken successor = new RefreshToken(token.getUser(), newTokenHash, newExpiresAt, token.getFamilyId());
        RefreshToken savedSuccessor = refreshTokenRepository.save(successor);

        token.setReplacedById(savedSuccessor.getId());
        refreshTokenRepository.save(token);

        return new TokenRotationResult(newRawToken, token.getUser());
    }

    /**
     * Revokes a single token on explicit logout.
     */
    @Transactional
    public void revokeToken(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return;
        }
        String tokenHash = hashToken(rawRefreshToken);
        refreshTokenRepository.findByTokenHash(tokenHash).ifPresent(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        });
    }

    /**
     * Revokes all active tokens for a specific user (e.g. password reset or global logout).
     */
    @Transactional
    public int revokeAllTokensForUser(Long userId) {
        return refreshTokenRepository.revokeAllByUserId(userId);
    }

    /**
     * Computes the SHA-256 hash of a raw token string for secure persistence.
     */
    public String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 digest algorithm unavailable in runtime", ex);
        }
    }

    /**
     * Generates a 256-bit cryptographically secure random token string.
     */
    private String generateRawTokenValue() {
        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }

    public record TokenPair(String rawToken, RefreshToken entity) {}

    public record TokenRotationResult(String newRawToken, User user) {}
}
