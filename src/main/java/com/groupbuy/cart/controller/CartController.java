package com.groupbuy.cart.controller;

import com.groupbuy.auth.security.UserPrincipal;
import com.groupbuy.cart.dto.*;
import com.groupbuy.cart.service.CartService;
import com.groupbuy.cart.service.CheckoutService;
import com.groupbuy.order.dto.OrderResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/cart")
@Tag(name = "Shopping Cart & Checkout", description = "Endpoints for managing buyer shopping carts and placing transactional checkout orders")
@SecurityRequirement(name = "BearerAuth")
public class CartController {

    private final CartService cartService;
    private final CheckoutService checkoutService;

    public CartController(CartService cartService, CheckoutService checkoutService) {
        this.cartService = cartService;
        this.checkoutService = checkoutService;
    }

    @GetMapping
    @Operation(summary = "View shopping cart", description = "Returns active cart contents with server-computed line item totals and subtotal.")
    public ResponseEntity<CartResponse> getCart(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(cartService.getCart(principal));
    }

    @PostMapping("/items")
    @Operation(summary = "Add item to cart", description = "Adds a product or campaign item to the cart, verifying live stock availability.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Item added to cart"),
            @ApiResponse(responseCode = "400", description = "Product out of stock or inactive"),
            @ApiResponse(responseCode = "404", description = "Product or campaign not found")
    })
    public ResponseEntity<CartResponse> addItem(
            @Valid @RequestBody AddToCartRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(cartService.addItem(request, principal));
    }

    @PutMapping("/items/{itemId}")
    @Operation(summary = "Update cart item quantity", description = "Modifies the quantity of an item in the cart, checking stock availability.")
    public ResponseEntity<CartResponse> updateItemQuantity(
            @PathVariable Long itemId,
            @Valid @RequestBody UpdateCartItemRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(cartService.updateItemQuantity(itemId, request, principal));
    }

    @DeleteMapping("/items/{itemId}")
    @Operation(summary = "Remove item from cart", description = "Removes a specific line item from the cart.")
    public ResponseEntity<CartResponse> removeItem(
            @PathVariable Long itemId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(cartService.removeItem(itemId, principal));
    }

    @DeleteMapping
    @Operation(summary = "Clear shopping cart", description = "Removes all items from the current user's shopping cart.")
    public ResponseEntity<Map<String, String>> clearCart(@AuthenticationPrincipal UserPrincipal principal) {
        cartService.clearCart(principal.getId());
        return ResponseEntity.ok(Map.of("message", "Shopping cart cleared successfully"));
    }

    @PostMapping("/checkout")
    @Operation(summary = "Checkout shopping cart", description = "Executes transactional checkout, locks inventory, recalculates prices on the server, and produces an order with immutable item snapshots.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Order created successfully"),
            @ApiResponse(responseCode = "400", description = "Cart empty or insufficient inventory to fulfill request")
    })
    public ResponseEntity<OrderResponse> checkout(
            @Valid @RequestBody CheckoutRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        OrderResponse response = checkoutService.checkout(request, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}

