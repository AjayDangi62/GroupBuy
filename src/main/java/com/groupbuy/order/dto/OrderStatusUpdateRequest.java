package com.groupbuy.order.dto;

import com.groupbuy.order.entity.Order;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request to update order status")
public record OrderStatusUpdateRequest(
        @NotNull(message = "Order status is required")
        @Schema(description = "New order status: CREATED, PAID, PROCESSING, SHIPPED, DELIVERED, CANCELLED", example = "PROCESSING")
        Order.Status status
) {
}

