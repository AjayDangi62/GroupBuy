package com.groupbuy.order.dto;

import com.groupbuy.order.entity.OrderItem;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Order line item with frozen historical price and product snapshot")
public record OrderItemResponse(
        @Schema(description = "Order item ID", example = "1")
        Long id,

        @Schema(description = "Original product ID", example = "10")
        Long productId,

        @Schema(description = "Campaign ID if purchased via campaign")
        Long campaignId,

        @Schema(description = "Historical snapshot of product title", example = "Sony WH-1000XM5")
        String productName,

        @Schema(description = "Historical snapshot of product SKU", example = "WH-1000XM5-BLK")
        String productSku,

        @Schema(description = "Settled unit price", example = "1800.00")
        BigDecimal unitPrice,

        @Schema(description = "Purchased quantity", example = "2")
        int quantity,

        @Schema(description = "Line subtotal", example = "3600.00")
        BigDecimal totalPrice
) {
    public static OrderItemResponse fromEntity(OrderItem item) {
        Long campaignId = item.getCampaign() != null ? item.getCampaign().getId() : null;
        return new OrderItemResponse(
                item.getId(),
                item.getProduct() != null ? item.getProduct().getId() : null,
                campaignId,
                item.getProductName(),
                item.getProductSku(),
                item.getUnitPrice(),
                item.getQuantity(),
                item.getTotalPrice()
        );
    }
}

