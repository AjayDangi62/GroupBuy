package com.groupbuy.category.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Category creation or update request")
public record CategoryRequest(
        @NotBlank(message = "Category name is required")
        @Size(max = 100, message = "Category name cannot exceed 100 characters")
        @Schema(description = "Category name", example = "Electronics")
        String name,

        @NotBlank(message = "Category slug is required")
        @Size(max = 120, message = "Category slug cannot exceed 120 characters")
        @Schema(description = "Unique category URL slug", example = "electronics")
        String slug,

        @Schema(description = "Category description", example = "Consumer gadgets, accessories, and tech items")
        String description,

        @Schema(description = "Parent category ID, null if top-level", example = "1")
        Long parentId,

        @Schema(description = "Whether the category is active and visible to buyers", example = "true")
        Boolean active,

        @Schema(description = "Display sort order index", example = "0")
        Integer displayOrder
) {
}

