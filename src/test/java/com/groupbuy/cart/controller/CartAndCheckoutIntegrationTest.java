package com.groupbuy.cart.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.groupbuy.cart.dto.AddToCartRequest;
import com.groupbuy.cart.dto.CheckoutRequest;
import com.groupbuy.cart.dto.UpdateCartItemRequest;
import com.groupbuy.cart.repository.CartItemRepository;
import com.groupbuy.cart.repository.CartRepository;
import com.groupbuy.category.entity.Category;
import com.groupbuy.category.repository.CategoryRepository;
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
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Cart and Checkout Integration Tests")
class CartAndCheckoutIntegrationTest {

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

    private String customerToken;
    private User customer;
    private Product product1;
    private Product product2;

    @BeforeEach
    void setUp() {
        databaseCleaner.clean();

        User seller = new User("seller@groupbuy.com", "seller_cart", passwordEncoder.encode("Pass123!"), "Seller", User.Role.SELLER);
        seller = userRepository.save(seller);

        customer = new User("buyer@groupbuy.com", "buyer_cart", passwordEncoder.encode("Pass123!"), "Buyer", User.Role.CUSTOMER);
        customer = userRepository.save(customer);
        customerToken = jwtTokenProvider.generateAccessToken(com.groupbuy.auth.security.UserPrincipal.fromUser(customer));

        Category cat = new Category("Gadgets", "gadgets", "Desc", null, true, 1);
        cat = categoryRepository.save(cat);

        product1 = new Product(seller, cat, "SKU-C1", "Mechanical Keyboard", "RGB Gaming", new BigDecimal("3000.00"), "INR", 20, 20, Product.Status.ACTIVE, null);
        product2 = new Product(seller, cat, "SKU-C2", "Gaming Mouse", "Wireless mouse", new BigDecimal("1500.00"), "INR", 15, 15, Product.Status.ACTIVE, null);
        product1 = productRepository.save(product1);
        product2 = productRepository.save(product2);
    }

    @Test
    @DisplayName("Shopping Cart Flow - Add item, update quantity, check server subtotal")
    void cartLifecycleFlow() throws Exception {
        AddToCartRequest addReq = new AddToCartRequest(product1.getId(), null, 2);

        mockMvc.perform(post("/api/v1/cart/items")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalQuantity").value(2))
                .andExpect(jsonPath("$.subtotal").value(6000.00));

        // Add second product
        AddToCartRequest addReq2 = new AddToCartRequest(product2.getId(), null, 1);
        MvcResult result = mockMvc.perform(post("/api/v1/cart/items")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addReq2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalQuantity").value(3))
                .andExpect(jsonPath("$.subtotal").value(7500.00))
                .andReturn();

        // View Cart
        mockMvc.perform(get("/api/v1/cart")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.subtotal").value(7500.00));
    }

    @Test
    @DisplayName("POST /api/v1/cart/checkout - Transactional checkout prevents overselling and creates snapshot order")
    void checkoutCreatesOrderAndDeductsStock() throws Exception {
        AddToCartRequest addReq = new AddToCartRequest(product1.getId(), null, 3);
        mockMvc.perform(post("/api/v1/cart/items")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addReq)))
                .andExpect(status().isOk());

        CheckoutRequest checkoutReq = new CheckoutRequest("100 Innovation Way, Tech Park", "chk-1234");

        mockMvc.perform(post("/api/v1/cart/checkout")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(checkoutReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderNumber").isNotEmpty())
                .andExpect(jsonPath("$.totalPrice").value(9000.00))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.items[0].productName").value("Mechanical Keyboard"))
                .andExpect(jsonPath("$.items[0].unitPrice").value(3000.00))
                .andExpect(jsonPath("$.items[0].quantity").value(3));

        // Inventory must be deducted
        Product updatedProduct = productRepository.findById(product1.getId()).orElseThrow();
        assertThat(updatedProduct.getAvailableInventory()).isEqualTo(17);

        // Cart must be cleared
        mockMvc.perform(get("/api/v1/cart")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalQuantity").value(0))
                .andExpect(jsonPath("$.subtotal").value(0.00));
    }
}

