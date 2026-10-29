package com.groupbuy.product.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

@Schema(description = "Product creation request payload")
public record ProductCreateRequest(
        @NotBlank(message = "Product SKU is required")
        @Size(max = 100, message = "SKU cannot exceed 100 characters")
        @Schema(description = "Stock Keeping Unit unique identifier", example = "WH-1000XM5-BLK")
        String sku,

        @NotBlank(message = "Product name is required")
        @Size(max = 255, message = "Product name cannot exceed 255 characters")
        @Schema(description = "Product display title", example = "Sony WH-1000XM5 Wireless Headphones")
        String name,

        @Schema(description = "Detailed product description", example = "Industry leading noise canceling headphones")
        String description,

        @NotNull(message = "Base price is required")
        @DecimalMin(value = "0.01", message = "Base price must be greater than zero")
        @Digits(integer = 10, fraction = 2, message = "Base price must have at most 2 decimal places")
        @Schema(description = "Baseline retail price per unit", example = "2499.00")
        BigDecimal basePrice,

        @Schema(description = "ISO 4217 Currency code", example = "INR")
        String currency,

        @NotNull(message = "Total inventory is required")
        @Min(value = 0, message = "Total inventory cannot be negative")
        @Schema(description = "Initial total inventory count", example = "100")
        Integer totalInventory,

        @Schema(description = "Associated category ID", example = "1")
        Long categoryId,

        @Schema(description = "URL to product thumbnail image", example = "https://example.com/images/headphones.jpg")
        String imageUrl
) {
}

