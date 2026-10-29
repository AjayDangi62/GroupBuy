package com.groupbuy.product.dto;

import com.groupbuy.product.entity.Product;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(description = "Product update request payload")
public record ProductUpdateRequest(
        @Size(max = 255, message = "Product name cannot exceed 255 characters")
        @Schema(description = "Updated product display name")
        String name,

        @Schema(description = "Updated product description")
        String description,

        @DecimalMin(value = "0.01", message = "Base price must be greater than zero")
        @Digits(integer = 10, fraction = 2, message = "Base price must have at most 2 decimal places")
        @Schema(description = "Updated base price")
        BigDecimal basePrice,

        @Min(value = 0, message = "Total inventory cannot be negative")
        @Schema(description = "Updated total inventory")
        Integer totalInventory,

        @Min(value = 0, message = "Available inventory cannot be negative")
        @Schema(description = "Updated available inventory")
        Integer availableInventory,

        @Schema(description = "Updated category ID")
        Long categoryId,

        @Schema(description = "Updated status: ACTIVE, ARCHIVED, OUT_OF_STOCK")
        Product.Status status,

        @Schema(description = "Updated image URL")
        String imageUrl
) {
}

