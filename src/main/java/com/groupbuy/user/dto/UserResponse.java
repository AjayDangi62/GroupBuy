package com.groupbuy.user.dto;

import com.groupbuy.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * DTO representing user account details for public/authenticated consumption.
 * Sensitive fields such as passwordHash are never included.
 */
@Schema(description = "User account profile details")
public record UserResponse(
        @Schema(description = "Unique user ID", example = "1")
        Long id,

        @Schema(description = "Email address", example = "alice@example.com")
        String email,

        @Schema(description = "Username", example = "alice_buyer")
        String username,

        @Schema(description = "Full name", example = "Alice Wonderland")
        String fullName,

        @Schema(description = "User role", example = "CUSTOMER")
        String role,

        @Schema(description = "Account status", example = "ACTIVE")
        String status,

        @Schema(description = "Account creation timestamp in UTC", example = "2026-01-01T12:00:00Z")
        Instant createdAt
) {
    public static UserResponse fromEntity(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getUsername(),
                user.getFullName(),
                user.getRole().name(),
                user.getStatus().name(),
                user.getCreatedAt()
        );
    }
}
