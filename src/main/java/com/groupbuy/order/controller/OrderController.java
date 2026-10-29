package com.groupbuy.order.controller;

import com.groupbuy.auth.security.UserPrincipal;
import com.groupbuy.order.dto.OrderResponse;
import com.groupbuy.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "Orders", description = "Endpoints for customer order history, details, and self-service cancellation")
@SecurityRequirement(name = "BearerAuth")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    @Operation(summary = "Get my order history", description = "Retrieves paginated orders for the authenticated customer.")
    public ResponseEntity<Page<OrderResponse>> getMyOrders(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(orderService.getUserOrders(principal, pageable));
    }

    @GetMapping("/{orderNumber}")
    @Operation(summary = "Get order details", description = "Retrieves order details and immutable line item snapshots by order number.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order details retrieved"),
            @ApiResponse(responseCode = "403", description = "Forbidden: order belongs to another customer"),
            @ApiResponse(responseCode = "404", description = "Order not found")
    })
    public ResponseEntity<OrderResponse> getOrder(
            @PathVariable String orderNumber,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(orderService.getOrderByNumber(orderNumber, principal));
    }

    @PostMapping("/{orderNumber}/cancel")
    @Operation(summary = "Cancel order", description = "Cancels an unfulfilled order and restores inventory stock to catalog.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order cancelled successfully"),
            @ApiResponse(responseCode = "400", description = "Order has already shipped or been delivered")
    })
    public ResponseEntity<OrderResponse> cancelOrder(
            @PathVariable String orderNumber,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(orderService.cancelOrder(orderNumber, principal));
    }
}

