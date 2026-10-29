package com.groupbuy.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for user registration.
 */
@Schema(description = "Registration request containing account credentials and profile information")
public record RegisterRequest(
        @Schema(description = "Valid email address (will be normalized to lowercase)", example = "alice@example.com")
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid email address")
        @Size(max = 255, message = "Email must not exceed 255 characters")
        String email,

        @Schema(description = "Unique username", example = "alice_buyer")
        @NotBlank(message = "Username is required")
        @Size(min = 3, max = 100, message = "Username must be between 3 and 100 characters")
        String username,

        @Schema(description = "Password (minimum 8 characters, must contain uppercase, lowercase, digit, and special character)", example = "SecureP@ss1")
        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 128, message = "Password must be between 8 and 128 characters")
        String password,

        @Schema(description = "Full legal name", example = "Alice Wonderland")
        @NotBlank(message = "Full name is required")
        @Size(max = 150, message = "Full name must not exceed 150 characters")
        String fullName,

        @Schema(description = "User role: CUSTOMER (default) or SELLER. ADMIN registration is not permitted.", example = "CUSTOMER")
        String role
) {
}
