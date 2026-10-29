package com.groupbuy.cart.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Checkout request payload")
public record CheckoutRequest(
        @NotBlank(message = "Shipping address is required")
        @Schema(description = "Destination shipping address for physical delivery", example = "42 Silicon Avenue, Tech City, 560001")
        String shippingAddress,

        @Schema(description = "Optional client idempotency key to prevent double checkout", example = "chk-9b8c7d6e-5f4a")
        String idempotencyKey
) {
}

