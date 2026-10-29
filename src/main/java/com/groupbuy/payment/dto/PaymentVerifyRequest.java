package com.groupbuy.payment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Server-side payment verification request")
public record PaymentVerifyRequest(
        @NotNull(message = "Payment ID is required")
        @Schema(description = "Payment ID returned during initiation", example = "1")
        Long paymentId,

        @NotBlank(message = "Gateway transaction ID is required")
        @Schema(description = "Transaction reference or token provided by payment gateway", example = "mtx_1a2b3c4d5e6f")
        String gatewayTransactionId,

        @Schema(description = "Cryptographic gateway signature (if applicable)", example = "a1b2c3d4...")
        String signature
) {
}

