package com.groupbuy.user.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.groupbuy.auth.dto.AuthResponse;
import com.groupbuy.auth.dto.RegisterRequest;
import com.groupbuy.auth.repository.RefreshTokenRepository;
import com.groupbuy.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("UserController Integration Tests")
class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private com.groupbuy.TestDatabaseCleaner databaseCleaner;

    @BeforeEach
    void cleanUp() {
        databaseCleaner.clean();
    }

    @Test
    @DisplayName("GET /api/v1/users/me - Returns 401 Unauthorized when unauthenticated")
    void getMeUnauthenticatedFails() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.errorCode").value("AUTHENTICATION_REQUIRED"));
    }

    @Test
    @DisplayName("GET /api/v1/users/me - Returns 401 Unauthorized with invalid or forged Bearer token")
    void getMeInvalidTokenFails() throws Exception {
        mockMvc.perform(get("/api/v1/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer invalid.jwt.token.here"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("GET /api/v1/users/me - Returns 200 OK with UserResponse for valid Bearer token")
    void getMeAuthenticatedSuccess() throws Exception {
        RegisterRequest registerReq = new RegisterRequest(
                "helen@example.com",
                "helen_k",
                "StrongP@ssw0rd!",
                "Helen Keller",
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

        mockMvc.perform(get("/api/v1/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + auth.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("helen@example.com"))
                .andExpect(jsonPath("$.username").value("helen_k"))
                .andExpect(jsonPath("$.fullName").value("Helen Keller"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());
    }
}
