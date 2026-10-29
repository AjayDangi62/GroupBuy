package com.groupbuy.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for user login.
 */
@Schema(description = "Login request containing email and password credentials")
public record LoginRequest(
        @Schema(description = "Registered email address", example = "alice@example.com")
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid email address")
        String email,

        @Schema(description = "Account password", example = "SecureP@ss1")
        @NotBlank(message = "Password is required")
        String password
) {
}
