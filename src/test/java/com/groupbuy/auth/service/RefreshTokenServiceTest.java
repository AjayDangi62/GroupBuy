package com.groupbuy.auth.service;

import com.groupbuy.auth.entity.RefreshToken;
import com.groupbuy.auth.repository.RefreshTokenRepository;
import com.groupbuy.common.exception.TokenRefreshException;
import com.groupbuy.config.JwtProperties;
import com.groupbuy.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RefreshTokenService Unit Tests")
class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private RefreshTokenService refreshTokenService;
    private User testUser;

    @BeforeEach
    void setUp() {
        JwtProperties jwtProperties = new JwtProperties("secretKeyWithAtLeast32BytesLength1234567890", 900000L, 604800000L, "groupbuy");
        refreshTokenService = new RefreshTokenService(refreshTokenRepository, jwtProperties);

        testUser = new User("alice@example.com", "alice", "hash", "Alice W", User.Role.CUSTOMER);
    }

    @Test
    @DisplayName("createRefreshToken creates and saves hashed token with family ID")
    void createRefreshTokenSuccess() {
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RefreshTokenService.TokenPair pair = refreshTokenService.createRefreshToken(testUser);

        assertThat(pair.rawToken()).isNotBlank();
        assertThat(pair.entity()).isNotNull();
        assertThat(pair.entity().getUser()).isEqualTo(testUser);
        assertThat(pair.entity().getFamilyId()).isNotBlank();
        assertThat(pair.entity().isRevoked()).isFalse();

        // Verify SHA-256 hash was persisted rather than raw token
        String expectedHash = refreshTokenService.hashToken(pair.rawToken());
        assertThat(pair.entity().getTokenHash()).isEqualTo(expectedHash);
    }

    @Test
    @DisplayName("rotateRefreshToken rotates valid token and maintains family ID")
    void rotateRefreshTokenSuccess() {
        String oldRawToken = "valid-raw-refresh-token-1234567890";
        String oldHash = refreshTokenService.hashToken(oldRawToken);
        String familyId = "family-xyz-123";

        RefreshToken oldToken = new RefreshToken(testUser, oldHash, Instant.now().plusSeconds(3600), familyId);

        when(refreshTokenRepository.findByTokenHash(oldHash)).thenReturn(Optional.of(oldToken));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RefreshTokenService.TokenRotationResult result = refreshTokenService.rotateRefreshToken(oldRawToken);

        assertThat(result.newRawToken()).isNotBlank();
        assertThat(result.newRawToken()).isNotEqualTo(oldRawToken);
        assertThat(result.user()).isEqualTo(testUser);
        assertThat(oldToken.isRevoked()).isTrue();

        // Successor should be saved in the same family
        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository, atLeastOnce()).save(captor.capture());

        RefreshToken successor = captor.getAllValues().stream()
                .filter(t -> !t.isRevoked())
                .findFirst()
                .orElse(null);
        assertThat(successor).isNotNull();
        assertThat(successor.getFamilyId()).isEqualTo(familyId);
    }

    @Test
    @DisplayName("rotateRefreshToken triggers reuse detection on revoked token and revokes entire family")
    void rotateRefreshTokenReuseDetection() {
        String reusedRawToken = "compromised-revoked-token";
        String tokenHash = refreshTokenService.hashToken(reusedRawToken);
        String familyId = "family-compromised-456";

        RefreshToken revokedToken = new RefreshToken(testUser, tokenHash, Instant.now().plusSeconds(3600), familyId);
        revokedToken.setRevoked(true);

        when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(revokedToken));

        assertThatThrownBy(() -> refreshTokenService.rotateRefreshToken(reusedRawToken))
                .isInstanceOf(TokenRefreshException.class)
                .hasMessageContaining("reuse detected");

        // Verify that all tokens in family were revoked
        verify(refreshTokenRepository).revokeAllByFamilyId(familyId);
    }

    @Test
    @DisplayName("rotateRefreshToken rejects expired token")
    void rotateRefreshTokenRejectsExpired() {
        String expiredRawToken = "expired-token-value";
        String tokenHash = refreshTokenService.hashToken(expiredRawToken);

        RefreshToken expiredToken = new RefreshToken(testUser, tokenHash, Instant.now().minusSeconds(3600), "fam-1");

        when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(expiredToken));

        assertThatThrownBy(() -> refreshTokenService.rotateRefreshToken(expiredRawToken))
                .isInstanceOf(TokenRefreshException.class)
                .hasMessageContaining("expired");
    }

    @Test
    @DisplayName("revokeToken invalidates active token")
    void revokeTokenSuccess() {
        String rawToken = "active-token-to-revoke";
        String tokenHash = refreshTokenService.hashToken(rawToken);

        RefreshToken token = new RefreshToken(testUser, tokenHash, Instant.now().plusSeconds(3600), "fam-1");
        when(refreshTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(token));

        refreshTokenService.revokeToken(rawToken);

        assertThat(token.isRevoked()).isTrue();
        verify(refreshTokenRepository).save(token);
    }
}
