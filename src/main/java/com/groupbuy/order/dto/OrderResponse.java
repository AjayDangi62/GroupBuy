package com.groupbuy.order.dto;

import com.groupbuy.order.entity.Order;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Schema(description = "Customer order details response")
public record OrderResponse(
        @Schema(description = "Order ID", example = "1")
        Long id,

        @Schema(description = "Unique human-readable order tracking number", example = "ORD-20260301-A1B2C3")
        String orderNumber,

        @Schema(description = "Buyer User ID", example = "3")
        Long userId,

        @Schema(description = "Shipping delivery address")
        String shippingAddress,

        @Schema(description = "Order financial total", example = "3600.00")
        BigDecimal totalPrice,

        @Schema(description = "Currency", example = "INR")
        String currency,

        @Schema(description = "Order status: CREATED, PAID, PROCESSING, SHIPPED, DELIVERED, CANCELLED", example = "PAID")
        String status,

        @Schema(description = "Order snapshot items")
        List<OrderItemResponse> items,

        @Schema(description = "Order placed timestamp")
        Instant createdAt,

        @Schema(description = "Order last updated timestamp")
        Instant updatedAt
) {
    public static OrderResponse fromEntity(Order order) {
        List<OrderItemResponse> items = order.getItems().stream()
                .map(OrderItemResponse::fromEntity)
                .toList();

        return new OrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getUser() != null ? order.getUser().getId() : null,
                order.getShippingAddress(),
                order.getTotalPrice(),
                order.getCurrency(),
                order.getStatus().name(),
                items,
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}

