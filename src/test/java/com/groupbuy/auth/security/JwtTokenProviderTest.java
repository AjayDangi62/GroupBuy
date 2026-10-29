package com.groupbuy.auth.security;

import com.groupbuy.config.JwtProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("JwtTokenProvider Unit Tests")
class JwtTokenProviderTest {

    private static final String TEST_SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private static final long ACCESS_EXPIRATION_MS = 3600000L; // 1 hour
    private static final long REFRESH_EXPIRATION_MS = 604800000L; // 7 days
    private static final String ISSUER = "groupbuy-test";

    private JwtTokenProvider jwtTokenProvider;
    private UserPrincipal testPrincipal;

    @BeforeEach
    void setUp() {
        JwtProperties jwtProperties = new JwtProperties(TEST_SECRET, ACCESS_EXPIRATION_MS, REFRESH_EXPIRATION_MS, ISSUER);
        jwtTokenProvider = new JwtTokenProvider(jwtProperties);

        testPrincipal = new UserPrincipal(
                42L,
                "test@example.com",
                "testuser",
                "hashedpassword",
                "Test User",
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_CUSTOMER")),
                true
        );
    }

    @Test
    @DisplayName("generateAccessToken produces valid, signed JWT with correct claims")
    void generateAccessTokenSuccess() {
        String token = jwtTokenProvider.generateAccessToken(testPrincipal);

        assertThat(token).isNotBlank();
        assertThat(jwtTokenProvider.validateToken(token)).isTrue();
        assertThat(jwtTokenProvider.getUserIdFromToken(token)).isEqualTo(42L);
        assertThat(jwtTokenProvider.getEmailFromToken(token)).isEqualTo("test@example.com");
        assertThat(jwtTokenProvider.getRolesFromToken(token)).containsExactly("ROLE_CUSTOMER");
    }

    @Test
    @DisplayName("validateToken returns false when token has been tampered with")
    void validateTokenRejectsTamperedToken() {
        String token = jwtTokenProvider.generateAccessToken(testPrincipal);
        String tamperedToken = token.substring(0, token.lastIndexOf('.') + 1) + "invalidsignature";

        assertThat(jwtTokenProvider.validateToken(tamperedToken)).isFalse();
    }

    @Test
    @DisplayName("validateToken returns false for expired tokens")
    void validateTokenRejectsExpiredToken() {
        SecretKey key = Keys.hmacShaKeyFor(TEST_SECRET.getBytes(StandardCharsets.UTF_8));
        String expiredToken = Jwts.builder()
                .subject("42")
                .issuer(ISSUER)
                .issuedAt(new Date(System.currentTimeMillis() - 7200000))
                .expiration(new Date(System.currentTimeMillis() - 3600000)) // expired 1 hr ago
                .signWith(key)
                .compact();

        assertThat(jwtTokenProvider.validateToken(expiredToken)).isFalse();
    }

    @Test
    @DisplayName("validateToken returns false when token signed by an unknown secret key")
    void validateTokenRejectsDifferentKey() {
        String foreignSecret = "DifferentSecretKeyForSigningThatIsAtLeast32BytesLong1234567890";
        SecretKey foreignKey = Keys.hmacShaKeyFor(foreignSecret.getBytes(StandardCharsets.UTF_8));

        String foreignToken = Jwts.builder()
                .subject("42")
                .issuer(ISSUER)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(foreignKey)
                .compact();

        assertThat(jwtTokenProvider.validateToken(foreignToken)).isFalse();
    }

    @Test
    @DisplayName("validateToken returns false for garbage string")
    void validateTokenRejectsGarbage() {
        assertThat(jwtTokenProvider.validateToken("not.a.valid.jwt")).isFalse();
        assertThat(jwtTokenProvider.validateToken("")).isFalse();
        assertThat(jwtTokenProvider.validateToken(null)).isFalse();
    }
}
