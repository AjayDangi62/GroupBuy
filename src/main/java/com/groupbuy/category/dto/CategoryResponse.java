package com.groupbuy.category.dto;

import com.groupbuy.category.entity.Category;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Category details response")
public record CategoryResponse(
        @Schema(description = "Category ID", example = "1")
        Long id,

        @Schema(description = "Category name", example = "Electronics")
        String name,

        @Schema(description = "Category slug", example = "electronics")
        String slug,

        @Schema(description = "Category description")
        String description,

        @Schema(description = "Parent category ID, null if root")
        Long parentId,

        @Schema(description = "Parent category name")
        String parentName,

        @Schema(description = "Whether category is active")
        boolean active,

        @Schema(description = "Sort order index")
        int displayOrder,

        @Schema(description = "Creation timestamp")
        Instant createdAt,

        @Schema(description = "Last update timestamp")
        Instant updatedAt
) {
    public static CategoryResponse fromEntity(Category category) {
        Long parentId = category.getParent() != null ? category.getParent().getId() : null;
        String parentName = category.getParent() != null ? category.getParent().getName() : null;
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getSlug(),
                category.getDescription(),
                parentId,
                parentName,
                category.isActive(),
                category.getDisplayOrder(),
                category.getCreatedAt(),
                category.getUpdatedAt()
        );
    }
}

