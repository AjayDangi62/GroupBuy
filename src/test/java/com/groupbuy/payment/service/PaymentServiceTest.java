package com.groupbuy.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.groupbuy.auth.security.UserPrincipal;
import com.groupbuy.common.exception.BusinessException;
import com.groupbuy.order.repository.OrderRepository;
import com.groupbuy.participation.repository.ParticipationRepository;
import com.groupbuy.payment.dto.PaymentInitiateRequest;
import com.groupbuy.payment.entity.Payment;
import com.groupbuy.payment.gateway.MockPaymentGateway;
import com.groupbuy.payment.gateway.PaymentGateway;
import com.groupbuy.payment.repository.*;
import com.groupbuy.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.env.MockEnvironment;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentService Unit Tests")
class PaymentServiceTest {

    @Mock private PaymentRepository paymentRepository;
    @Mock private PaymentAttemptRepository attemptRepository;
    @Mock private PaymentEventRepository eventRepository;
    @Mock private RefundRepository refundRepository;
    @Mock private IdempotencyRecordRepository idempotencyRecordRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private ParticipationRepository participationRepository;
    @Mock private UserRepository userRepository;

    private PaymentGateway paymentGateway = new MockPaymentGateway();
    private ObjectMapper objectMapper = new ObjectMapper();
    private MockEnvironment testEnvironment;
    private static final String TEST_SECRET = "groupbuy-webhook-mock-secret";
    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        testEnvironment = new MockEnvironment();
        testEnvironment.setActiveProfiles("test");
        paymentService = new PaymentService(
                paymentRepository, attemptRepository, eventRepository,
                refundRepository, idempotencyRecordRepository, orderRepository,
                participationRepository, userRepository, paymentGateway, objectMapper,
                testEnvironment, TEST_SECRET
        );
    }

    @Test
    @DisplayName("initiatePayment rejects request with neither orderId nor participationId")
    void initiatePaymentRejectsMissingTarget() {
        PaymentInitiateRequest request = new PaymentInitiateRequest(null, null, "key-1");
        UserPrincipal principal = new UserPrincipal(1L, "user@test.com", "user", "pass", "User", List.of(), true);

        assertThatThrownBy(() -> paymentService.initiatePayment(request, principal))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Either orderId or participationId must be provided");
    }

    @Test
    @DisplayName("processWebhook rejects invalid HMAC signature")
    void processWebhookRejectsInvalidSignature() {
        String payload = "{\"event\":\"payment.captured\",\"transactionId\":\"tx_1\"}";
        String invalidSignature = "invalid_hex_signature";

        assertThatThrownBy(() -> paymentService.processWebhook(payload, invalidSignature))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Invalid webhook signature");
    }

    @Test
    @DisplayName("processWebhook rejects missing or blank signature header")
    void processWebhookRejectsMissingSignature() {
        String payload = "{\"event\":\"payment.captured\",\"transactionId\":\"tx_1\"}";

        assertThatThrownBy(() -> paymentService.processWebhook(payload, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Invalid webhook signature");

        assertThatThrownBy(() -> paymentService.processWebhook(payload, "   "))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Invalid webhook signature");
    }

    @Test
    @DisplayName("processWebhook accepts valid HMAC signature and processes captured event")
    void processWebhookAcceptsValidSignature() throws Exception {
        String payload = "{\"event\":\"payment.captured\",\"transactionId\":\"tx_mock_123\",\"amount\":1500.00,\"currency\":\"INR\"}";
        String secret = "groupbuy-webhook-mock-secret";

        javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
        mac.init(new javax.crypto.spec.SecretKeySpec(secret.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256"));
        String validSignature = java.util.HexFormat.of().formatHex(mac.doFinal(payload.getBytes(java.nio.charset.StandardCharsets.UTF_8)));

        Payment payment = new Payment(null, new BigDecimal("1500.00"), "INR", "MOCK", "idemp-1");
        when(paymentRepository.findByTransactionId("tx_mock_123")).thenReturn(Optional.of(payment));

        boolean processed = paymentService.processWebhook(payload, validSignature);
        assertThat(processed).isTrue();
        assertThat(payment.getStatus()).isEqualTo(Payment.Status.CAPTURED);
        verify(eventRepository).save(any());
        verify(paymentRepository).save(payment);
    }

    @Test
    @DisplayName("processWebhook rejects missing or blank secret in production environment")
    void processWebhookRejectsMissingSecretInProduction() {
        MockEnvironment prodEnv = new MockEnvironment();
        prodEnv.setActiveProfiles("prod");
        PaymentService prodService = new PaymentService(
                paymentRepository, attemptRepository, eventRepository,
                refundRepository, idempotencyRecordRepository, orderRepository,
                participationRepository, userRepository, paymentGateway, objectMapper,
                prodEnv, ""
        );
        String payload = "{\"event\":\"payment.captured\",\"transactionId\":\"tx_1\"}";

        assertThatThrownBy(() -> prodService.processWebhook(payload, "sig_mock"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Payment webhook secret is not configured");
    }

    @Test
    @DisplayName("processWebhook rejects insecure or mock secret in production environment")
    void processWebhookRejectsInsecureSecretInProduction() {
        MockEnvironment prodEnv = new MockEnvironment();
        prodEnv.setActiveProfiles("prod");
        PaymentService prodService = new PaymentService(
                paymentRepository, attemptRepository, eventRepository,
                refundRepository, idempotencyRecordRepository, orderRepository,
                participationRepository, userRepository, paymentGateway, objectMapper,
                prodEnv, "groupbuy-webhook-mock-secret"
        );
        String payload = "{\"event\":\"payment.captured\",\"transactionId\":\"tx_1\"}";

        assertThatThrownBy(() -> prodService.processWebhook(payload, "sig_mock"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Production requires a strong externally configured webhook secret");
    }

    @Test
    @DisplayName("processWebhook allows mock secret in explicitly configured local/test environment")
    void processWebhookAllowsMockSecretInLocalEnvironment() throws Exception {
        MockEnvironment localEnv = new MockEnvironment();
        localEnv.setActiveProfiles("local");
        String mockSecret = "groupbuy-webhook-mock-secret";
        PaymentService localService = new PaymentService(
                paymentRepository, attemptRepository, eventRepository,
                refundRepository, idempotencyRecordRepository, orderRepository,
                participationRepository, userRepository, paymentGateway, objectMapper,
                localEnv, mockSecret
        );

        String payload = "{\"event\":\"payment.captured\",\"transactionId\":\"tx_mock_local\",\"amount\":1200.00,\"currency\":\"INR\"}";
        javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
        mac.init(new javax.crypto.spec.SecretKeySpec(mockSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256"));
        String validSignature = java.util.HexFormat.of().formatHex(mac.doFinal(payload.getBytes(java.nio.charset.StandardCharsets.UTF_8)));

        Payment payment = new Payment(null, new BigDecimal("1200.00"), "INR", "MOCK", "idemp-local");
        when(paymentRepository.findByTransactionId("tx_mock_local")).thenReturn(Optional.of(payment));

        boolean processed = localService.processWebhook(payload, validSignature);
        assertThat(processed).isTrue();
    }

    @Test
    @DisplayName("processWebhook accepts valid signature in production when strong secret is configured")
    void processWebhookAcceptsValidSignatureInProduction() throws Exception {
        MockEnvironment prodEnv = new MockEnvironment();
        prodEnv.setActiveProfiles("prod");
        String strongSecret = "c8f93a10b45d2e76f1890123456789abcdef0123456789abcdef0123456789ab";
        PaymentService prodService = new PaymentService(
                paymentRepository, attemptRepository, eventRepository,
                refundRepository, idempotencyRecordRepository, orderRepository,
                participationRepository, userRepository, paymentGateway, objectMapper,
                prodEnv, strongSecret
        );

        String payload = "{\"event\":\"payment.captured\",\"transactionId\":\"tx_prod_123\",\"amount\":2500.00,\"currency\":\"INR\"}";
        javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
        mac.init(new javax.crypto.spec.SecretKeySpec(strongSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256"));
        String validSignature = java.util.HexFormat.of().formatHex(mac.doFinal(payload.getBytes(java.nio.charset.StandardCharsets.UTF_8)));

        Payment payment = new Payment(null, new BigDecimal("2500.00"), "INR", "MOCK", "idemp-prod");
        when(paymentRepository.findByTransactionId("tx_prod_123")).thenReturn(Optional.of(payment));

        boolean processed = prodService.processWebhook(payload, validSignature);
        assertThat(processed).isTrue();
        assertThat(payment.getStatus()).isEqualTo(Payment.Status.CAPTURED);
    }
}
