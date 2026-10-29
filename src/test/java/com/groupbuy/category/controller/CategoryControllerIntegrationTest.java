package com.groupbuy.category.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.groupbuy.auth.dto.AuthResponse;
import com.groupbuy.auth.dto.RegisterRequest;
import com.groupbuy.category.dto.CategoryRequest;
import com.groupbuy.category.repository.CategoryRepository;
import com.groupbuy.user.entity.User;
import com.groupbuy.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Category Controller Integration Tests")
class CategoryControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private com.groupbuy.TestDatabaseCleaner databaseCleaner;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private com.groupbuy.auth.security.JwtTokenProvider jwtTokenProvider;

    private String adminToken;
    private String customerToken;

    @BeforeEach
    void setUp() {
        databaseCleaner.clean();

        // Create an ADMIN user
        User admin = new User("admin@groupbuy.com", "admin_user", passwordEncoder.encode("AdminPass123!"), "Administrator", User.Role.ADMIN);
        User savedAdmin = userRepository.save(admin);
        adminToken = jwtTokenProvider.generateAccessToken(com.groupbuy.auth.security.UserPrincipal.fromUser(savedAdmin));

        // Create a CUSTOMER user
        User customer = new User("customer@groupbuy.com", "cust_user", passwordEncoder.encode("CustPass123!"), "Customer One", User.Role.CUSTOMER);
        User savedCustomer = userRepository.save(customer);
        customerToken = jwtTokenProvider.generateAccessToken(com.groupbuy.auth.security.UserPrincipal.fromUser(savedCustomer));
    }

    @Test
    @DisplayName("POST /api/v1/admin/categories - Successfully creates category with ADMIN role")
    void adminCanCreateCategory() throws Exception {
        CategoryRequest request = new CategoryRequest(
                "Electronics",
                "electronics",
                "Electronic devices and accessories",
                null,
                true,
                1
        );

        mockMvc.perform(post("/api/v1/admin/categories")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Electronics"))
                .andExpect(jsonPath("$.slug").value("electronics"))
                .andExpect(jsonPath("$.active").value(true));

        assertThat(categoryRepository.findBySlug("electronics")).isPresent();
    }

    @Test
    @DisplayName("POST /api/v1/admin/categories - Rejects creation from regular CUSTOMER with 403 Forbidden")
    void customerCannotCreateCategory() throws Exception {
        CategoryRequest request = new CategoryRequest(
                "Books",
                "books",
                "Printed and digital books",
                null,
                true,
                2
        );

        mockMvc.perform(post("/api/v1/admin/categories")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/v1/categories - Returns public active categories without authentication")
    void publicCanBrowseActiveCategories() throws Exception {
        CategoryRequest request = new CategoryRequest(
                "Home & Kitchen",
                "home-kitchen",
                "Appliances and cookware",
                null,
                true,
                1
        );

        mockMvc.perform(post("/api/v1/admin/categories")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].slug").value("home-kitchen"))
                .andExpect(jsonPath("$[0].name").value("Home & Kitchen"));
    }

    @Test
    @DisplayName("POST /api/v1/admin/categories - Rejects duplicate slug with 409 Conflict")
    void duplicateSlugRejected() throws Exception {
        CategoryRequest request = new CategoryRequest("Audio", "audio", "Headphones", null, true, 1);
        mockMvc.perform(post("/api/v1/admin/categories")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/admin/categories")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("CATEGORY_SLUG_EXISTS"));
    }
}

