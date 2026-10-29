package com.groupbuy.payment.dto;

import com.groupbuy.payment.entity.Payment;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

@Schema(description = "Payment processing outcome details")
public record PaymentResponse(
        @Schema(description = "Payment ID", example = "1")
        Long id,

        @Schema(description = "Associated Order ID if applicable", example = "10")
        Long orderId,

        @Schema(description = "Associated Participation ID if applicable", example = "5")
        Long participationId,

        @Schema(description = "Authorized amount", example = "3600.00")
        BigDecimal authorizedAmount,

        @Schema(description = "Captured amount", example = "3200.00")
        BigDecimal capturedAmount,

        @Schema(description = "Refunded amount", example = "400.00")
        BigDecimal refundedAmount,

        @Schema(description = "Currency", example = "INR")
        String currency,

        @Schema(description = "Payment lifecycle status: PENDING, AUTHORIZED, CAPTURED, REFUNDED, FAILED", example = "AUTHORIZED")
        String status,

        @Schema(description = "Provider name", example = "MOCK_LOCAL_GATEWAY")
        String paymentGateway,

        @Schema(description = "Provider transaction identifier", example = "mtx_1a2b3c4d")
        String transactionId,

        @Schema(description = "Hosted payment or redirect URL (if awaiting customer auth)")
        String paymentUrl,

        @Schema(description = "Creation timestamp")
        Instant createdAt
) {
    public static PaymentResponse fromEntity(Payment payment) {
        return fromEntity(payment, null);
    }

    public static PaymentResponse fromEntity(Payment payment, String paymentUrl) {
        Long orderId = payment.getOrder() != null ? payment.getOrder().getId() : null;
        Long participationId = payment.getParticipation() != null ? payment.getParticipation().getId() : null;

        return new PaymentResponse(
                payment.getId(),
                orderId,
                participationId,
                payment.getAuthorizedAmount(),
                payment.getCapturedAmount(),
                payment.getRefundedAmount(),
                payment.getCurrency(),
                payment.getStatus().name(),
                payment.getPaymentGateway(),
                payment.getTransactionId(),
                paymentUrl,
                payment.getCreatedAt()
        );
    }
}

