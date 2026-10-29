package com.groupbuy.product.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.groupbuy.category.entity.Category;
import com.groupbuy.category.repository.CategoryRepository;
import com.groupbuy.product.dto.ProductCreateRequest;
import com.groupbuy.product.dto.ProductUpdateRequest;
import com.groupbuy.product.entity.Product;
import com.groupbuy.product.repository.ProductRepository;
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

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Product Controller Integration Tests")
class ProductControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private com.groupbuy.TestDatabaseCleaner databaseCleaner;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private com.groupbuy.auth.security.JwtTokenProvider jwtTokenProvider;

    private String sellerToken;
    private String customerToken;
    private User seller;
    private Category category;

    @BeforeEach
    void setUp() {
        databaseCleaner.clean();

        seller = new User("seller@groupbuy.com", "seller_one", passwordEncoder.encode("SellerPass123!"), "Acme Seller", User.Role.SELLER);
        seller = userRepository.save(seller);
        sellerToken = jwtTokenProvider.generateAccessToken(com.groupbuy.auth.security.UserPrincipal.fromUser(seller));

        User customer = new User("buyer@groupbuy.com", "buyer_one", passwordEncoder.encode("BuyerPass123!"), "Buyer One", User.Role.CUSTOMER);
        customer = userRepository.save(customer);
        customerToken = jwtTokenProvider.generateAccessToken(com.groupbuy.auth.security.UserPrincipal.fromUser(customer));

        category = new Category("Electronics", "electronics", "Tech gadgets", null, true, 1);
        category = categoryRepository.save(category);
    }

    @Test
    @DisplayName("POST /api/v1/products - SELLER can successfully create a new product")
    void sellerCanCreateProduct() throws Exception {
        ProductCreateRequest request = new ProductCreateRequest(
                "WH-1000XM5-BLK",
                "Sony WH-1000XM5",
                "Noise canceling headphones",
                new BigDecimal("2499.00"),
                "INR",
                50,
                category.getId(),
                "https://images.groupbuy.local/headphones.png"
        );

        mockMvc.perform(post("/api/v1/products")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + sellerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sku").value("WH-1000XM5-BLK"))
                .andExpect(jsonPath("$.name").value("Sony WH-1000XM5"))
                .andExpect(jsonPath("$.basePrice").value(2499.00))
                .andExpect(jsonPath("$.totalInventory").value(50))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        assertThat(productRepository.findBySku("WH-1000XM5-BLK")).isPresent();
    }

    @Test
    @DisplayName("POST /api/v1/products - Rejects CUSTOMER role with 403 Forbidden")
    void customerCannotCreateProduct() throws Exception {
        ProductCreateRequest request = new ProductCreateRequest(
                "SKU-CUST",
                "Unauthorized Product",
                null,
                new BigDecimal("100.00"),
                "INR",
                10,
                category.getId(),
                null
        );

        mockMvc.perform(post("/api/v1/products")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/v1/products - Rejects invalid price <= 0 with 400 Bad Request")
    void rejectNegativeOrZeroPrice() throws Exception {
        ProductCreateRequest request = new ProductCreateRequest(
                "SKU-NEG",
                "Negative Price Product",
                null,
                new BigDecimal("-50.00"),
                "INR",
                10,
                category.getId(),
                null
        );

        mockMvc.perform(post("/api/v1/products")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + sellerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/products - Public listing supports null search, query search, category filtering, and pagination")
    void publicCanBrowseProducts() throws Exception {
        Product p1 = new Product(seller, category, "SKU-P1", "Wireless Earbuds", "Bluetooth earbuds", new BigDecimal("1200.00"), "INR", 30, 30, Product.Status.ACTIVE, null);
        Product p2 = new Product(seller, category, "SKU-P2", "Smart Speaker", "Home assistant speaker", new BigDecimal("1800.00"), "INR", 20, 20, Product.Status.ACTIVE, null);
        productRepository.save(p1);
        productRepository.save(p2);

        // 1. GET /api/v1/products with NO search parameter (null query) returns HTTP 200 with all active items
        mockMvc.perform(get("/api/v1/products")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(2));

        // 2. GET /api/v1/products?search=Earbuds with non-empty search query returns HTTP 200 with filtered items
        mockMvc.perform(get("/api/v1/products?search=Earbuds")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Wireless Earbuds"));

        // 3. GET /api/v1/products?categoryId=... returns HTTP 200
        mockMvc.perform(get("/api/v1/products?categoryId=" + category.getId())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.content.length()").value(2));
    }

    @Test
    @DisplayName("PUT /api/v1/products/{id} - Seller can update own product details")
    void sellerCanUpdateProduct() throws Exception {
        Product p = new Product(seller, category, "SKU-UP", "Old Title", "Old Desc", new BigDecimal("1000.00"), "INR", 10, 10, Product.Status.ACTIVE, null);
        Product saved = productRepository.save(p);

        ProductUpdateRequest update = new ProductUpdateRequest("New Title", "New Desc", new BigDecimal("1100.00"), 15, 15, null, null, null);

        mockMvc.perform(put("/api/v1/products/" + saved.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + sellerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New Title"))
                .andExpect(jsonPath("$.basePrice").value(1100.00));
    }
}

