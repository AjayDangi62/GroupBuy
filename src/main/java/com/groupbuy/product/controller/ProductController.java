package com.groupbuy.product.controller;

import com.groupbuy.auth.security.UserPrincipal;
import com.groupbuy.product.dto.ProductCreateRequest;
import com.groupbuy.product.dto.ProductResponse;
import com.groupbuy.product.dto.ProductUpdateRequest;
import com.groupbuy.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/products")
@Tag(name = "Products", description = "Endpoints for catalog browsing and seller product management")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    @Operation(summary = "Browse public products", description = "Search, filter by category, and paginate through active products.")
    public ResponseEntity<Page<ProductResponse>> getProducts(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long categoryId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(productService.getPublicProducts(search, categoryId, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get product details", description = "Retrieves complete product details by ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Product details retrieved"),
            @ApiResponse(responseCode = "404", description = "Product not found")
    })
    public ResponseEntity<ProductResponse> getProductById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getProductById(id));
    }

    @GetMapping("/seller/me")
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    @Operation(summary = "List seller's products", description = "Retrieves all products belonging to the authenticated seller.", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<Page<ProductResponse>> getMyProducts(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(productService.getSellerProducts(principal.getId(), pageable));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    @Operation(summary = "Create product", description = "Creates a new catalog product. Only users with SELLER or ADMIN role may perform this.", security = @SecurityRequirement(name = "BearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Product created successfully"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "409", description = "Product SKU already exists")
    })
    public ResponseEntity<ProductResponse> createProduct(
            @Valid @RequestBody ProductCreateRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        ProductResponse response = productService.createProduct(request, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    @Operation(summary = "Update product", description = "Updates an existing product. Only the owning seller or an admin may edit.", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductUpdateRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(productService.updateProduct(id, request, principal));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('SELLER', 'ADMIN')")
    @Operation(summary = "Delete product", description = "Archives a product. Only the owning seller or an admin may delete.", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<Map<String, String>> deleteProduct(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        productService.deleteProduct(id, principal);
        return ResponseEntity.ok(Map.of("message", "Product archived successfully"));
    }
}

