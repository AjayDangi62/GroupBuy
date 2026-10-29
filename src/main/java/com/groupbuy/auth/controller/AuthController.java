package com.groupbuy.auth.controller;

import com.groupbuy.auth.dto.AuthResponse;
import com.groupbuy.auth.dto.LoginRequest;
import com.groupbuy.auth.dto.RefreshTokenRequest;
import com.groupbuy.auth.dto.RegisterRequest;
import com.groupbuy.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Controller exposing endpoints for user registration, authentication, token refresh, and logout.
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Endpoints for user registration, authentication, token refresh, and session revocation")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new user account", description = "Creates a new user profile with CUSTOMER (default) or SELLER role and returns initial JWT tokens.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User successfully registered and authenticated"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload or weak password"),
            @ApiResponse(responseCode = "409", description = "Email or username is already taken")
    })
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate user credentials", description = "Validates email and password, returning an access token and refresh token upon success.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Authentication successful"),
            @ApiResponse(responseCode = "400", description = "Missing or malformed request payload"),
            @ApiResponse(responseCode = "401", description = "Invalid email or password"),
            @ApiResponse(responseCode = "403", description = "Account is suspended or deactivated")
    })
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Rotate refresh token", description = "Exchanges a valid refresh token for a newly rotated token pair. Detects token reuse.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tokens rotated successfully"),
            @ApiResponse(responseCode = "400", description = "Missing refresh token"),
            @ApiResponse(responseCode = "401", description = "Invalid, expired, or compromised refresh token")
    })
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    @Operation(summary = "Log out user session", description = "Revokes the active refresh token.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Session revoked successfully")
    })
    public ResponseEntity<Map<String, String>> logout(@RequestBody(required = false) RefreshTokenRequest request) {
        if (request != null && request.refreshToken() != null) {
            authService.logout(request.refreshToken());
        }
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }
}
