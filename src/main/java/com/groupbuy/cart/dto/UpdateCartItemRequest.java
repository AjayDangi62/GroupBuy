package com.groupbuy.cart.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(description = "Request to update quantity of an existing cart item")
public record UpdateCartItemRequest(
        @NotNull(message = "Quantity is required")
        @Positive(message = "Quantity must be greater than zero")
        @Schema(description = "New quantity for item", example = "3")
        Integer quantity
) {
}

