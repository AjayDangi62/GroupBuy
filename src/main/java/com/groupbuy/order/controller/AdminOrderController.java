package com.groupbuy.order.controller;

import com.groupbuy.order.dto.OrderResponse;
import com.groupbuy.order.dto.OrderStatusUpdateRequest;
import com.groupbuy.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/orders")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Operations - Orders", description = "Administrative endpoints for viewing and advancing order statuses across the platform")
@SecurityRequirement(name = "BearerAuth")
public class AdminOrderController {

    private final OrderService orderService;

    public AdminOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    @Operation(summary = "List all orders (Admin)", description = "Retrieves all platform orders with pagination and sorting.")
    public ResponseEntity<Page<OrderResponse>> getAllOrders(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(orderService.getAllOrders(pageable));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update order status (Admin)", description = "Transitions an order state (CREATED -> PAID -> PROCESSING -> SHIPPED -> DELIVERED, or CANCELLED).")
    public ResponseEntity<OrderResponse> updateOrderStatus(
            @PathVariable Long id,
            @Valid @RequestBody OrderStatusUpdateRequest request
    ) {
        return ResponseEntity.ok(orderService.updateOrderStatus(id, request));
    }
}

