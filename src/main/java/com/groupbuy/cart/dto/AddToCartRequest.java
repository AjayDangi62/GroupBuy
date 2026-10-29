package com.groupbuy.cart.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(description = "Request to add a product or campaign item to the shopping cart")
public record AddToCartRequest(
        @NotNull(message = "Product ID is required")
        @Schema(description = "Product ID to add", example = "1")
        Long productId,

        @Schema(description = "Optional group-buy campaign ID if purchasing via campaign", example = "2")
        Long campaignId,

        @NotNull(message = "Quantity is required")
        @Positive(message = "Quantity must be greater than zero")
        @Schema(description = "Quantity to add", example = "1")
        Integer quantity
) {
}

