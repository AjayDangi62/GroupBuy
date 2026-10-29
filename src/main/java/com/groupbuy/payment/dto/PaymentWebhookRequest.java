package com.groupbuy.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.util.Map;

@Schema(description = "Payment gateway incoming webhook payload")
public record PaymentWebhookRequest(
        @NotBlank(message = "Event type is required")
        @Schema(description = "Event type: payment.authorized, payment.captured, payment.failed, refund.processed", example = "payment.captured")
        String event,

        @NotBlank(message = "Transaction ID is required")
        @Schema(description = "Gateway transaction ID", example = "mtx_1a2b3c4d5e6f")
        String transactionId,

        @Schema(description = "Payment amount", example = "1800.00")
        BigDecimal amount,

        @Schema(description = "Currency", example = "INR")
        String currency,

        @Schema(description = "Payment status", example = "CAPTURED")
        String status,

        @Schema(description = "Additional gateway metadata")
        Map<String, Object> metadata
) {
}

