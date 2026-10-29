package com.groupbuy.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Payment session initiation request")
public record PaymentInitiateRequest(
        @Schema(description = "Associated Order ID (if paying for regular checkout order)", example = "1")
        Long orderId,

        @Schema(description = "Associated Participation ID (if paying for campaign commitment)", example = "5")
        Long participationId,

        @Schema(description = "Optional client idempotency key to prevent double charge", example = "pay-key-12345")
        String idempotencyKey
) {
}

