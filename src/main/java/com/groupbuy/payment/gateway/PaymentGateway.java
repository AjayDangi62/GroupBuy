package com.groupbuy.payment.gateway;

import java.math.BigDecimal;
import java.util.Map;

public interface PaymentGateway {

    String getProviderName();

    PaymentInitiationResult initiatePayment(PaymentInitiationCommand command);

    PaymentVerificationResult verifyPayment(PaymentVerificationCommand command);

    RefundResult processRefund(RefundCommand command);

    boolean verifyWebhookSignature(String payload, String signature, String secret);

    record PaymentInitiationCommand(
            String transactionReference,
            BigDecimal amount,
            String currency,
            String customerEmail,
            String description,
            Map<String, String> metadata
    ) {}

    record PaymentInitiationResult(
            boolean success,
            String gatewayTransactionId,
            String paymentUrl,
            String clientSecret,
            String status,
            String message
    ) {}

    record PaymentVerificationCommand(
            String gatewayTransactionId,
            String paymentReference,
            String signature
    ) {}

    record PaymentVerificationResult(
            boolean verified,
            String status,
            BigDecimal capturedAmount,
            String transactionId,
            String failureReason
    ) {}

    record RefundCommand(
            String gatewayTransactionId,
            BigDecimal amount,
            String currency,
            String reason
    ) {}

    record RefundResult(
            boolean success,
            String refundReference,
            BigDecimal refundedAmount,
            String status,
            String message
    ) {}
}

