package com.groupbuy.payment.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;

/**
 * Local / Test simulation mode for payment processing.
 *
 * <p>Used in local development and automated testing environments where live third-party
 * banking/payment credentials (e.g. Stripe, Razorpay) are not provisioned.
 * Generates realistic deterministic transaction identifiers and supports HMAC-SHA256
 * cryptographic webhook signature verification.
 */
@Component
public class MockPaymentGateway implements PaymentGateway {

    private static final Logger log = LoggerFactory.getLogger(MockPaymentGateway.class);
    public static final String PROVIDER_NAME = "MOCK_LOCAL_GATEWAY";

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }

    @Override
    public PaymentInitiationResult initiatePayment(PaymentInitiationCommand command) {
        log.info("[Payment Gateway: Local Mode] Initiating payment for ref: {}, amount: {} {}",
                command.transactionReference(), command.amount(), command.currency());

        String gatewayTxId = "mtx_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        String clientSecret = "sec_" + UUID.randomUUID().toString().replace("-", "");
        String paymentUrl = "https://checkout.groupbuy.local/pay/" + gatewayTxId;

        return new PaymentInitiationResult(
                true,
                gatewayTxId,
                paymentUrl,
                clientSecret,
                "INITIATED",
                "Mock payment session initiated successfully in local test mode"
        );
    }

    @Override
    public PaymentVerificationResult verifyPayment(PaymentVerificationCommand command) {
        log.info("[Payment Gateway: Local Mode] Verifying payment for txId: {}", command.gatewayTransactionId());

        if (command.gatewayTransactionId() == null || command.gatewayTransactionId().isBlank()) {
            return new PaymentVerificationResult(false, "FAILED", BigDecimal.ZERO, null, "Missing transaction ID");
        }

        // If transaction ID contains 'fail', simulate gateway failure
        if (command.gatewayTransactionId().contains("fail")) {
            return new PaymentVerificationResult(false, "FAILED", BigDecimal.ZERO, command.gatewayTransactionId(), "Simulated payment failure (declined by bank)");
        }

        return new PaymentVerificationResult(
                true,
                "AUTHORIZED",
                BigDecimal.ZERO,
                command.gatewayTransactionId(),
                null
        );
    }

    @Override
    public RefundResult processRefund(RefundCommand command) {
        log.info("[Payment Gateway: Local Mode] Processing refund for txId: {}, amount: {}",
                command.gatewayTransactionId(), command.amount());

        String refundRef = "rfnd_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        return new RefundResult(
                true,
                refundRef,
                command.amount(),
                "PROCESSED",
                "Mock refund processed successfully"
        );
    }

    @Override
    public boolean verifyWebhookSignature(String payload, String signature, String secret) {
        if (payload == null || signature == null || secret == null) {
            return false;
        }

        try {
            Mac sha256Hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            sha256Hmac.init(secretKey);
            byte[] hash = sha256Hmac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String expectedSignature = HexFormat.of().formatHex(hash);

            return MessageDigest.isEqual(
                    expectedSignature.getBytes(StandardCharsets.UTF_8),
                    signature.getBytes(StandardCharsets.UTF_8)
            );
        } catch (Exception ex) {
            log.warn("HMAC signature verification failed: {}", ex.getMessage());
            return false;
        }
    }
}

