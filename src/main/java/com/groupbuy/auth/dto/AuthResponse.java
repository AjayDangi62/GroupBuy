package com.groupbuy.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response DTO returned upon successful authentication or token refresh.
 */
@Schema(description = "Authentication response containing JWT access token, refresh token, and user metadata")
public record AuthResponse(
        @Schema(description = "JWT Access Token for Bearer authentication", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
        String accessToken,

        @Schema(description = "Opaque cryptographically-secure refresh token for session extension", example = "a7b3c2d1-e4f5-4a6b-8c9d-0e1f2a3b4c5d")
        String refreshToken,

        @Schema(description = "Token type", example = "Bearer")
        String tokenType,

        @Schema(description = "Access token lifetime in seconds", example = "900")
        long expiresIn,

        @Schema(description = "Authenticated user ID", example = "1")
        Long userId,

        @Schema(description = "User email address", example = "alice@example.com")
        String email,

        @Schema(description = "User username", example = "alice_buyer")
        String username,

        @Schema(description = "User full name", example = "Alice Wonderland")
        String fullName,

        @Schema(description = "Assigned user role", example = "CUSTOMER")
        String role
) {
    public static AuthResponse of(
            String accessToken,
            String refreshToken,
            long expiresInSeconds,
            Long userId,
            String email,
            String username,
            String fullName,
            String role
    ) {
        return new AuthResponse(
                accessToken,
                refreshToken,
                "Bearer",
                expiresInSeconds,
                userId,
                email,
                username,
                fullName,
                role
        );
    }
}
