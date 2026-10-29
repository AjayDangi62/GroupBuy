package com.groupbuy.user.controller;

import com.groupbuy.auth.security.UserPrincipal;
import com.groupbuy.common.exception.ResourceNotFoundException;
import com.groupbuy.user.dto.UserResponse;
import com.groupbuy.user.entity.User;
import com.groupbuy.user.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller exposing endpoints for accessing user profile data.
 */
@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "User Management", description = "Endpoints for accessing user profile data")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/me")
    @Operation(
            summary = "Get current user profile",
            description = "Returns profile details of the currently authenticated user.",
            security = @SecurityRequirement(name = "BearerAuth")
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User profile retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthenticated - missing or invalid JWT")
    })
    public ResponseEntity<UserResponse> getCurrentUser(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            throw new ResourceNotFoundException("No authenticated user principal found");
        }

        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + principal.getId()));

        return ResponseEntity.ok(UserResponse.fromEntity(user));
    }
}
