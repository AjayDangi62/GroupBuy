package com.groupbuy.product.dto;

import com.groupbuy.product.entity.Product;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

@Schema(description = "Product details response payload")
public record ProductResponse(
        @Schema(description = "Product ID", example = "1")
        Long id,

        @Schema(description = "Seller user ID", example = "2")
        Long sellerId,

        @Schema(description = "Seller full name", example = "Acme Electronics")
        String sellerName,

        @Schema(description = "Category ID", example = "1")
        Long categoryId,

        @Schema(description = "Category name", example = "Electronics")
        String categoryName,

        @Schema(description = "SKU", example = "WH-1000XM5-BLK")
        String sku,

        @Schema(description = "Product name", example = "Sony WH-1000XM5")
        String name,

        @Schema(description = "Product description")
        String description,

        @Schema(description = "Base price", example = "2499.00")
        BigDecimal basePrice,

        @Schema(description = "Currency", example = "INR")
        String currency,

        @Schema(description = "Total inventory count", example = "100")
        int totalInventory,

        @Schema(description = "Available inventory count", example = "95")
        int availableInventory,

        @Schema(description = "Product status", example = "ACTIVE")
        String status,

        @Schema(description = "Product image URL")
        String imageUrl,

        @Schema(description = "Creation timestamp")
        Instant createdAt,

        @Schema(description = "Last update timestamp")
        Instant updatedAt
) {
    public static ProductResponse fromEntity(Product product) {
        Long sellerId = product.getSeller() != null ? product.getSeller().getId() : null;
        String sellerName = product.getSeller() != null ? product.getSeller().getFullName() : null;
        Long categoryId = product.getCategory() != null ? product.getCategory().getId() : null;
        String categoryName = product.getCategory() != null ? product.getCategory().getName() : null;

        return new ProductResponse(
                product.getId(),
                sellerId,
                sellerName,
                categoryId,
                categoryName,
                product.getSku(),
                product.getName(),
                product.getDescription(),
                product.getBasePrice(),
                product.getCurrency(),
                product.getTotalInventory(),
                product.getAvailableInventory(),
                product.getStatus().name(),
                product.getImageUrl(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }
}

