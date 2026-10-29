package com.groupbuy.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.groupbuy.auth.dto.AuthResponse;
import com.groupbuy.auth.dto.LoginRequest;
import com.groupbuy.auth.dto.RefreshTokenRequest;
import com.groupbuy.auth.dto.RegisterRequest;
import com.groupbuy.auth.repository.RefreshTokenRepository;
import com.groupbuy.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("AuthController Integration Tests")
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private com.groupbuy.TestDatabaseCleaner databaseCleaner;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void cleanUp() {
        databaseCleaner.clean();
    }

    @Test
    @DisplayName("POST /api/v1/auth/register - Successfully registers user and returns token pair with 201 Created")
    void registerSuccess() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "charlie@example.com",
                "charlie_buyer",
                "StrongP@ssw0rd!",
                "Charlie Brown",
                "CUSTOMER"
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.email").value("charlie@example.com"))
                .andExpect(jsonPath("$.username").value("charlie_buyer"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"));

        assertThat(userRepository.findByEmail("charlie@example.com")).isPresent();
    }

    @Test
    @DisplayName("POST /api/v1/auth/register - Rejects duplicate email with 409 Conflict")
    void registerDuplicateEmailReturns409() throws Exception {
        RegisterRequest initial = new RegisterRequest(
                "duplicate@example.com",
                "user1",
                "StrongP@ssw0rd!",
                "User One",
                "CUSTOMER"
        );
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(initial)))
                .andExpect(status().isCreated());

        RegisterRequest duplicate = new RegisterRequest(
                "duplicate@example.com",
                "user2",
                "StrongP@ssw0rd!",
                "User Two",
                "CUSTOMER"
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicate)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("EMAIL_ALREADY_EXISTS"))
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - Returns tokens on valid credentials")
    void loginSuccess() throws Exception {
        // Register first
        RegisterRequest registerReq = new RegisterRequest(
                "david@example.com",
                "david_seller",
                "StrongP@ssw0rd!",
                "David Seller",
                "SELLER"
        );
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated());

        // Login
        LoginRequest loginReq = new LoginRequest("david@example.com", "StrongP@ssw0rd!");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.role").value("SELLER"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - Returns 401 Unauthorized on invalid password")
    void loginInvalidPasswordReturns401() throws Exception {
        RegisterRequest registerReq = new RegisterRequest(
                "eve@example.com",
                "eve_buyer",
                "StrongP@ssw0rd!",
                "Eve Buyer",
                "CUSTOMER"
        );
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated());

        LoginRequest loginReq = new LoginRequest("eve@example.com", "WrongPassword!");
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("POST /api/v1/auth/refresh - Successfully rotates refresh token and detects reuse")
    void refreshRotationAndReuseDetection() throws Exception {
        // 1. Register to obtain initial token pair
        RegisterRequest registerReq = new RegisterRequest(
                "frank@example.com",
                "frank_buyer",
                "StrongP@ssw0rd!",
                "Frank Buyer",
                "CUSTOMER"
        );
        MvcResult registerResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andReturn();

        AuthResponse initialAuth = objectMapper.readValue(
                registerResult.getResponse().getContentAsString(),
                AuthResponse.class
        );
        String initialRefreshToken = initialAuth.refreshToken();

        // 2. Refresh token rotation - exchange initial refresh token for a new pair
        RefreshTokenRequest refreshReq = new RefreshTokenRequest(initialRefreshToken);
        MvcResult refreshResult = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andReturn();

        AuthResponse rotatedAuth = objectMapper.readValue(
                refreshResult.getResponse().getContentAsString(),
                AuthResponse.class
        );
        String rotatedRefreshToken = rotatedAuth.refreshToken();
        assertThat(rotatedRefreshToken).isNotEqualTo(initialRefreshToken);

        // 3. REUSE DETECTION: Replaying the initial (already rotated) token must fail with 401
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest(initialRefreshToken))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("TOKEN_REUSE_DETECTED"));

        // 4. Because reuse was detected, the successor token in the family must also now be revoked!
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest(rotatedRefreshToken))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("TOKEN_REUSE_DETECTED"));
    }

    @Test
    @DisplayName("POST /api/v1/auth/logout - Revokes active refresh token")
    void logoutRevokesToken() throws Exception {
        RegisterRequest registerReq = new RegisterRequest(
                "grace@example.com",
                "grace_buyer",
                "StrongP@ssw0rd!",
                "Grace Hopper",
                "CUSTOMER"
        );
        MvcResult registerResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerReq)))
                .andExpect(status().isCreated())
                .andReturn();

        AuthResponse auth = objectMapper.readValue(
                registerResult.getResponse().getContentAsString(),
                AuthResponse.class
        );

        // Logout
        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest(auth.refreshToken()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(is("Logged out successfully")));

        // Attempting to refresh with the logged-out token should fail
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest(auth.refreshToken()))))
                .andExpect(status().isUnauthorized());
    }
}
