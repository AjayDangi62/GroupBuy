package com.groupbuy.order.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.groupbuy.category.entity.Category;
import com.groupbuy.category.repository.CategoryRepository;
import com.groupbuy.order.dto.OrderStatusUpdateRequest;
import com.groupbuy.order.entity.Order;
import com.groupbuy.order.entity.OrderItem;
import com.groupbuy.order.repository.OrderItemRepository;
import com.groupbuy.order.repository.OrderRepository;
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
@DisplayName("Order Controller Integration Tests")
class OrderControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private com.groupbuy.TestDatabaseCleaner databaseCleaner;

    @Autowired
    private OrderRepository orderRepository;

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

    private String customer1Token;
    private String customer2Token;
    private String adminToken;
    private User customer1;
    private User customer2;
    private Product product;

    @BeforeEach
    void setUp() {
        databaseCleaner.clean();

        User seller = new User("seller@groupbuy.com", "seller_ord", passwordEncoder.encode("Pass123!"), "Seller", User.Role.SELLER);
        seller = userRepository.save(seller);

        User admin = new User("admin@groupbuy.com", "admin_ord", passwordEncoder.encode("Pass123!"), "Admin", User.Role.ADMIN);
        admin = userRepository.save(admin);
        adminToken = jwtTokenProvider.generateAccessToken(com.groupbuy.auth.security.UserPrincipal.fromUser(admin));

        customer1 = new User("c1@groupbuy.com", "cust1_ord", passwordEncoder.encode("Pass123!"), "Customer One", User.Role.CUSTOMER);
        customer1 = userRepository.save(customer1);
        customer1Token = jwtTokenProvider.generateAccessToken(com.groupbuy.auth.security.UserPrincipal.fromUser(customer1));

        customer2 = new User("c2@groupbuy.com", "cust2_ord", passwordEncoder.encode("Pass123!"), "Customer Two", User.Role.CUSTOMER);
        customer2 = userRepository.save(customer2);
        customer2Token = jwtTokenProvider.generateAccessToken(com.groupbuy.auth.security.UserPrincipal.fromUser(customer2));

        Category cat = new Category("Wearables", "wearables", "Desc", null, true, 1);
        cat = categoryRepository.save(cat);

        product = new Product(seller, cat, "SKU-WATCH", "Fitness Watch", "Smart tracker", new BigDecimal("5000.00"), "INR", 10, 10, Product.Status.ACTIVE, null);
        product = productRepository.save(product);
    }

    @Test
    @DisplayName("GET /api/v1/orders/{orderNumber} - Buyer can access own order; other buyers are rejected with 403 Forbidden")
    void customerCanOnlyViewOwnOrder() throws Exception {
        Order order = new Order("ORD-TEST-001", customer1, "Address 1", new BigDecimal("5000.00"), "INR");
        order.addItem(new OrderItem(order, product, null, "Fitness Watch", "SKU-WATCH", new BigDecimal("5000.00"), 1));
        orderRepository.save(order);

        // Customer 1 (owner) can view
        mockMvc.perform(get("/api/v1/orders/ORD-TEST-001")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customer1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderNumber").value("ORD-TEST-001"))
                .andExpect(jsonPath("$.totalPrice").value(5000.00));

        // Customer 2 (stranger) is blocked
        mockMvc.perform(get("/api/v1/orders/ORD-TEST-001")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customer2Token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /api/v1/orders/{orderNumber}/cancel - Cancelling order restores product inventory")
    void cancellingOrderRestoresInventory() throws Exception {
        // Product inventory was 10, after placing order of 2 units it becomes 8
        product.deductStock(2);
        productRepository.save(product);

        Order order = new Order("ORD-TEST-002", customer1, "Address 1", new BigDecimal("10000.00"), "INR");
        order.addItem(new OrderItem(order, product, null, "Fitness Watch", "SKU-WATCH", new BigDecimal("5000.00"), 2));
        orderRepository.save(order);

        mockMvc.perform(post("/api/v1/orders/ORD-TEST-002/cancel")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customer1Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        Product restored = productRepository.findById(product.getId()).orElseThrow();
        assertThat(restored.getAvailableInventory()).isEqualTo(10);
    }

    @Test
    @DisplayName("PATCH /api/v1/admin/orders/{id}/status - Admin can update order status")
    void adminCanUpdateOrderStatus() throws Exception {
        Order order = new Order("ORD-TEST-003", customer1, "Address 1", new BigDecimal("5000.00"), "INR");
        Order saved = orderRepository.save(order);

        OrderStatusUpdateRequest req = new OrderStatusUpdateRequest(Order.Status.PROCESSING);

        mockMvc.perform(patch("/api/v1/admin/orders/" + saved.getId() + "/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PROCESSING"));
    }
}

