package com.groupbuy.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.groupbuy.auth.security.UserPrincipal;
import com.groupbuy.common.constants.CurrencyConstants;
import com.groupbuy.common.exception.BusinessException;
import com.groupbuy.common.exception.ResourceNotFoundException;
import com.groupbuy.order.entity.Order;
import com.groupbuy.order.repository.OrderRepository;
import com.groupbuy.participation.entity.Participation;
import com.groupbuy.participation.repository.ParticipationRepository;
import com.groupbuy.payment.dto.*;
import com.groupbuy.payment.entity.*;
import com.groupbuy.payment.gateway.PaymentGateway;
import com.groupbuy.payment.repository.*;
import com.groupbuy.user.entity.User;
import com.groupbuy.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;

@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;
    private final PaymentAttemptRepository attemptRepository;
    private final PaymentEventRepository eventRepository;
    private final RefundRepository refundRepository;
    private final IdempotencyRecordRepository idempotencyRecordRepository;
    private final OrderRepository orderRepository;
    private final ParticipationRepository participationRepository;
    private final UserRepository userRepository;
    private final PaymentGateway paymentGateway;
    private final ObjectMapper objectMapper;

    private final Environment environment;
    private final String webhookSecret;

    public PaymentService(PaymentRepository paymentRepository,
                          PaymentAttemptRepository attemptRepository,
                          PaymentEventRepository eventRepository,
                          RefundRepository refundRepository,
                          IdempotencyRecordRepository idempotencyRecordRepository,
                          OrderRepository orderRepository,
                          ParticipationRepository participationRepository,
                          UserRepository userRepository,
                          PaymentGateway paymentGateway,
                          ObjectMapper objectMapper,
                          Environment environment,
                          @Value("${app.payment.webhook-secret:}") String webhookSecret) {
        this.paymentRepository = paymentRepository;
        this.attemptRepository = attemptRepository;
        this.eventRepository = eventRepository;
        this.refundRepository = refundRepository;
        this.idempotencyRecordRepository = idempotencyRecordRepository;
        this.orderRepository = orderRepository;
        this.participationRepository = participationRepository;
        this.userRepository = userRepository;
        this.paymentGateway = paymentGateway;
        this.objectMapper = objectMapper;
        this.environment = environment;
        this.webhookSecret = webhookSecret != null ? webhookSecret.trim() : "";
    }

    @Transactional
    public PaymentResponse initiatePayment(PaymentInitiateRequest request, UserPrincipal principal) {
        // Idempotency check
        if (request.idempotencyKey() != null && !request.idempotencyKey().isBlank()) {
            Optional<Payment> existing = paymentRepository.findByIdempotencyKey(request.idempotencyKey());
            if (existing.isPresent()) {
                log.info("Idempotent payment initiation returned for key: {}", request.idempotencyKey());
                return PaymentResponse.fromEntity(existing.get());
            }
        }

        BigDecimal amount;
        String currency;
        String refDescription;
        Order order = null;
        Participation participation = null;

        if (request.orderId() != null) {
            order = orderRepository.findById(request.orderId())
                    .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + request.orderId()));
            amount = order.getTotalPrice();
            currency = order.getCurrency();
            refDescription = "Payment for Order " + order.getOrderNumber();
        } else if (request.participationId() != null) {
            participation = participationRepository.findById(request.participationId())
                    .orElseThrow(() -> new ResourceNotFoundException("Participation not found with id: " + request.participationId()));
            amount = participation.getAuthorizedTotalAmount();
            currency = participation.getCampaign().getCurrency();
            refDescription = "Authorization hold for Campaign: " + participation.getCampaign().getTitle();
        } else {
            throw new BusinessException("Either orderId or participationId must be provided to initiate payment", "MISSING_PAYMENT_TARGET");
        }

        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + principal.getId()));

        PaymentGateway.PaymentInitiationCommand cmd = new PaymentGateway.PaymentInitiationCommand(
                "GB-" + System.currentTimeMillis(),
                amount,
                currency,
                user.getEmail(),
                refDescription,
                Map.of("userId", String.valueOf(user.getId()))
        );

        PaymentGateway.PaymentInitiationResult result = paymentGateway.initiatePayment(cmd);
        if (!result.success()) {
            throw new BusinessException("Payment gateway failed to initialize session: " + result.message(), "PAYMENT_INITIATION_FAILED");
        }

        Payment payment = new Payment(user, amount, currency, paymentGateway.getProviderName(), request.idempotencyKey());
        payment.setOrder(order);
        payment.setParticipation(participation);
        payment.setTransactionId(result.gatewayTransactionId());
        payment.setStatus(Payment.Status.PENDING);

        Payment saved = paymentRepository.save(payment);

        PaymentAttempt attempt = new PaymentAttempt(
                saved,
                1,
                result.gatewayTransactionId(),
                amount,
                PaymentAttempt.Status.INITIATED
        );
        attemptRepository.save(attempt);

        return PaymentResponse.fromEntity(saved, result.paymentUrl());
    }

    @Transactional
    public PaymentResponse verifyPayment(PaymentVerifyRequest request, UserPrincipal principal) {
        Payment payment = paymentRepository.findById(request.paymentId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + request.paymentId()));

        if (!payment.getUser().getId().equals(principal.getId())) {
            throw new BusinessException("Payment does not belong to authenticated user", "UNAUTHORIZED_PAYMENT_ACCESS");
        }

        PaymentGateway.PaymentVerificationCommand cmd = new PaymentGateway.PaymentVerificationCommand(
                request.gatewayTransactionId(),
                String.valueOf(payment.getId()),
                request.signature()
        );

        PaymentGateway.PaymentVerificationResult result = paymentGateway.verifyPayment(cmd);

        PaymentAttempt attempt = new PaymentAttempt(
                payment,
                payment.getAttempts().size() + 1,
                request.gatewayTransactionId(),
                payment.getAuthorizedAmount(),
                result.verified() ? PaymentAttempt.Status.SUCCESS : PaymentAttempt.Status.FAILURE
        );
        if (!result.verified()) {
            attempt.setErrorMessage(result.failureReason());
            attempt.setErrorCode("VERIFICATION_DECLINED");
            payment.setStatus(Payment.Status.FAILED);
            attemptRepository.save(attempt);
            paymentRepository.save(payment);
            throw new BusinessException("Payment verification failed: " + result.failureReason(), "PAYMENT_VERIFICATION_FAILED");
        }

        attemptRepository.save(attempt);

        payment.setStatus(Payment.Status.AUTHORIZED);
        payment.setCapturedAmount(payment.getAuthorizedAmount());

        // Advance associated entity state
        if (payment.getOrder() != null) {
            payment.getOrder().setStatus(Order.Status.PAID);
            orderRepository.save(payment.getOrder());
        }
        if (payment.getParticipation() != null) {
            payment.getParticipation().setStatus(Participation.Status.CONFIRMED);
            participationRepository.save(payment.getParticipation());
        }

        Payment saved = paymentRepository.save(payment);
        return PaymentResponse.fromEntity(saved);
    }

    @Transactional
    public boolean processWebhook(String rawPayload, String signatureHeader) {
        validateWebhookConfiguration();

        // Validate webhook signature
        if (signatureHeader == null || signatureHeader.isBlank()) {
            log.warn("Rejected incoming webhook with missing or blank signature header");
            throw new BusinessException("Invalid webhook signature: missing signature header", "INVALID_WEBHOOK_SIGNATURE");
        }

        boolean valid = paymentGateway.verifyWebhookSignature(rawPayload, signatureHeader, webhookSecret);
        if (!valid) {
            log.warn("Rejected incoming webhook with invalid HMAC signature: {}", signatureHeader);
            throw new BusinessException("Invalid webhook signature", "INVALID_WEBHOOK_SIGNATURE");
        }

        try {
            PaymentWebhookRequest webhook = objectMapper.readValue(rawPayload, PaymentWebhookRequest.class);

            Optional<Payment> paymentOpt = paymentRepository.findByTransactionId(webhook.transactionId());
            if (paymentOpt.isEmpty()) {
                log.warn("Webhook received for unknown transaction ID: {}", webhook.transactionId());
                return false;
            }

            Payment payment = paymentOpt.get();

            // Idempotent event handling: log audit event
            PaymentEvent event = new PaymentEvent(payment, webhook.event(), rawPayload);
            eventRepository.save(event);

            // State update based on event type
            if ("payment.captured".equalsIgnoreCase(webhook.event())) {
                payment.setStatus(Payment.Status.CAPTURED);
                if (webhook.amount() != null) {
                    payment.setCapturedAmount(webhook.amount());
                }
                if (payment.getOrder() != null) {
                    payment.getOrder().setStatus(Order.Status.PAID);
                    orderRepository.save(payment.getOrder());
                }
            } else if ("payment.failed".equalsIgnoreCase(webhook.event())) {
                payment.setStatus(Payment.Status.FAILED);
            }

            paymentRepository.save(payment);
            return true;
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to parse or process webhook payload", ex);
            throw new BusinessException("Webhook processing error: " + ex.getMessage(), "WEBHOOK_PROCESSING_ERROR");
        }
    }

    @Transactional
    public void captureAndRefundDifferential(Payment payment, BigDecimal settledUnitPrice, int quantity) {
        BigDecimal settledTotal = settledUnitPrice.multiply(BigDecimal.valueOf(quantity))
                .setScale(CurrencyConstants.MONETARY_SCALE, CurrencyConstants.DEFAULT_ROUNDING_MODE);

        payment.setCapturedAmount(settledTotal);

        BigDecimal differential = payment.getAuthorizedAmount().subtract(settledTotal);
        if (differential.compareTo(BigDecimal.ZERO) > 0) {
            // Process differential refund
            PaymentGateway.RefundCommand refundCmd = new PaymentGateway.RefundCommand(
                    payment.getTransactionId(),
                    differential,
                    payment.getCurrency(),
                    "GroupBuy Volume Discount Differential Refund"
            );
            PaymentGateway.RefundResult refundResult = paymentGateway.processRefund(refundCmd);

            Refund refund = new Refund(
                    payment,
                    refundResult.refundReference(),
                    differential,
                    payment.getCurrency(),
                    "GroupBuy settled price lower than initial authorization"
            );
            refund.setStatus(Refund.Status.PROCESSED);
            refundRepository.save(refund);

            payment.setRefundedAmount(differential);
            payment.setStatus(Payment.Status.CAPTURED);
        } else {
            payment.setStatus(Payment.Status.CAPTURED);
        }

        paymentRepository.save(payment);
    }

    private void validateWebhookConfiguration() {
        if (webhookSecret == null || webhookSecret.isBlank()) {
            log.error("Payment webhook secret is not configured");
            throw new BusinessException("Payment webhook secret is not configured", "WEBHOOK_SECRET_NOT_CONFIGURED");
        }

        boolean isLocalOrTest = isLocalOrTestEnvironment();
        if (!isLocalOrTest && isKnownMockOrWeakSecret(webhookSecret)) {
            log.error("Insecure or mock webhook secret detected in non-test environment");
            throw new BusinessException("Production requires a strong externally configured webhook secret", "INSECURE_WEBHOOK_SECRET");
        }
    }

    private boolean isLocalOrTestEnvironment() {
        if (environment == null) {
            return false;
        }
        for (String profile : environment.getActiveProfiles()) {
            if ("test".equalsIgnoreCase(profile) || "local".equalsIgnoreCase(profile) || "dev".equalsIgnoreCase(profile)) {
                return true;
            }
        }
        for (String profile : environment.getDefaultProfiles()) {
            if ("test".equalsIgnoreCase(profile) || "local".equalsIgnoreCase(profile) || "dev".equalsIgnoreCase(profile)) {
                return true;
            }
        }
        return false;
    }

    private boolean isKnownMockOrWeakSecret(String secret) {
        if (secret == null || secret.isBlank()) {
            return true;
        }
        String lower = secret.trim().toLowerCase();
        return lower.contains("mock")
                || lower.contains("test")
                || lower.equals("secret")
                || lower.equals("changeme")
                || lower.equals("password")
                || secret.trim().length() < 32;
    }
}

