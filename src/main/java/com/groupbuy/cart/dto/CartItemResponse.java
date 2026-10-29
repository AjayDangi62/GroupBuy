package com.groupbuy.cart.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Shopping cart line item")
public record CartItemResponse(
        @Schema(description = "Cart Item ID", example = "5")
        Long id,

        @Schema(description = "Product ID", example = "1")
        Long productId,

        @Schema(description = "Product Name", example = "Sony WH-1000XM5")
        String productName,

        @Schema(description = "Product SKU", example = "WH-1000XM5-BLK")
        String productSku,

        @Schema(description = "Product image URL")
        String productImageUrl,

        @Schema(description = "Campaign ID if group-buy item, null if regular retail purchase")
        Long campaignId,

        @Schema(description = "Campaign title if applicable")
        String campaignTitle,

        @Schema(description = "Selected quantity", example = "2")
        int quantity,

        @Schema(description = "Server-calculated unit price", example = "1800.00")
        BigDecimal unitPrice,

        @Schema(description = "Server-calculated line total (unitPrice * quantity)", example = "3600.00")
        BigDecimal lineTotal,

        @Schema(description = "Currency", example = "INR")
        String currency
) {
}

