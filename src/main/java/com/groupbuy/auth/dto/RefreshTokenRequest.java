package com.groupbuy.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for exchanging an existing refresh token for a newly rotated token pair.
 */
@Schema(description = "Request to rotate access and refresh tokens")
public record RefreshTokenRequest(
        @Schema(description = "Valid refresh token issued by the auth service", example = "a7b3c2d1-e4f5-4a6b-8c9d-0e1f2a3b4c5d")
        @NotBlank(message = "Refresh token is required")
        String refreshToken
) {
}
