package com.groupbuy.payment.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.groupbuy.order.entity.Order;
import com.groupbuy.order.repository.OrderRepository;
import com.groupbuy.payment.dto.PaymentInitiateRequest;
import com.groupbuy.payment.dto.PaymentVerifyRequest;
import com.groupbuy.payment.entity.Payment;
import com.groupbuy.payment.entity.PaymentEvent;
import com.groupbuy.payment.gateway.MockPaymentGateway;
import com.groupbuy.payment.repository.PaymentEventRepository;
import com.groupbuy.payment.repository.PaymentRepository;
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

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Payment Controller Integration Tests")
class PaymentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private com.groupbuy.TestDatabaseCleaner databaseCleaner;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PaymentEventRepository eventRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private com.groupbuy.auth.security.JwtTokenProvider jwtTokenProvider;

    private String customerToken;
    private User customer;
    private Order order;

    @BeforeEach
    void setUp() {
        databaseCleaner.clean();

        customer = new User("buyer@groupbuy.com", "buyer_pay", passwordEncoder.encode("Pass123!"), "Buyer Pay", User.Role.CUSTOMER);
        customer = userRepository.save(customer);
        customerToken = jwtTokenProvider.generateAccessToken(com.groupbuy.auth.security.UserPrincipal.fromUser(customer));

        order = new Order("ORD-PAY-001", customer, "Shipping Addr", new BigDecimal("3500.00"), "INR");
        order = orderRepository.save(order);
    }

    @Test
    @DisplayName("Payment Flow - Initiate, verify, advance order to PAID")
    void paymentInitiateAndVerifyFlow() throws Exception {
        PaymentInitiateRequest initReq = new PaymentInitiateRequest(order.getId(), null, "idemp-001");

        MvcResult initResult = mockMvc.perform(post("/api/v1/payments/initiate")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(initReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.authorizedAmount").value(3500.00))
                .andExpect(jsonPath("$.paymentUrl").isNotEmpty())
                .andReturn();

        Long paymentId = objectMapper.readTree(initResult.getResponse().getContentAsString()).get("id").asLong();
        String txId = objectMapper.readTree(initResult.getResponse().getContentAsString()).get("transactionId").asText();

        // Client completes payment and server verifies
        PaymentVerifyRequest verifyReq = new PaymentVerifyRequest(paymentId, txId, "mock_signature");

        mockMvc.perform(post("/api/v1/payments/verify")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AUTHORIZED"));

        // Order status must be advanced to PAID
        Order updatedOrder = orderRepository.findById(order.getId()).orElseThrow();
        assertThat(updatedOrder.getStatus()).isEqualTo(Order.Status.PAID);
    }

    @Test
    @DisplayName("POST /api/v1/payments/webhook - Processes gateway webhook with HMAC cryptographic signature")
    void webhookSignatureVerification() throws Exception {
        // Create an existing payment record
        Payment payment = new Payment(customer, new BigDecimal("2000.00"), "INR", MockPaymentGateway.PROVIDER_NAME, "idemp-web");
        payment.setTransactionId("mtx_test_webhook_123");
        paymentRepository.save(payment);

        String payload = """
                {
                    "event": "payment.captured",
                    "transactionId": "mtx_test_webhook_123",
                    "amount": 2000.00,
                    "currency": "INR",
                    "status": "CAPTURED"
                }
                """;

        String secret = "groupbuy-webhook-mock-secret";
        Mac sha256 = Mac.getInstance("HmacSHA256");
        sha256.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String signature = HexFormat.of().formatHex(sha256.doFinal(payload.getBytes(StandardCharsets.UTF_8)));

        mockMvc.perform(post("/api/v1/payments/webhook")
                        .header("X-Webhook-Signature", signature)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.received").value(true))
                .andExpect(jsonPath("$.processed").value(true));

        Payment updated = paymentRepository.findByTransactionId("mtx_test_webhook_123").orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(Payment.Status.CAPTURED);

        java.util.List<PaymentEvent> events = eventRepository.findByPaymentIdOrderByCreatedAtDesc(updated.getId());
        assertThat(events).hasSize(1);
        assertThat(events.get(0).getEventType()).isEqualTo("payment.captured");
        assertThat(events.get(0).getPayload()).contains("mtx_test_webhook_123");
    }

    @Test
    @DisplayName("PaymentEvent entity safely persists and retrieves structured JSON payload")
    void paymentEventJsonbPersistence() {
        Payment payment = new Payment(customer, new BigDecimal("1000.00"), "INR", MockPaymentGateway.PROVIDER_NAME, "idemp-jsonb");
        payment.setTransactionId("mtx_jsonb_001");
        payment = paymentRepository.save(payment);

        String jsonPayload = "{\"gateway\":\"mock\",\"code\":200,\"nested\":{\"verified\":true}}";
        PaymentEvent event = new PaymentEvent(payment, "payment.verified", jsonPayload);
        PaymentEvent saved = eventRepository.save(event);

        PaymentEvent retrieved = eventRepository.findById(saved.getId()).orElseThrow();
        assertThat(retrieved.getPayload()).isEqualTo(jsonPayload);
        assertThat(retrieved.getEventType()).isEqualTo("payment.verified");
    }
}

