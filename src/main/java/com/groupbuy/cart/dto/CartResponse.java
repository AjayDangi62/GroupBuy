package com.groupbuy.cart.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "User shopping cart breakdown with server-calculated totals")
public record CartResponse(
        @Schema(description = "Cart ID", example = "1")
        Long id,

        @Schema(description = "Buyer User ID", example = "3")
        Long userId,

        @Schema(description = "Cart line items")
        List<CartItemResponse> items,

        @Schema(description = "Total number of physical items in cart", example = "4")
        int totalQuantity,

        @Schema(description = "Server-calculated subtotal across all items", example = "5400.00")
        BigDecimal subtotal,

        @Schema(description = "Currency", example = "INR")
        String currency
) {
}

