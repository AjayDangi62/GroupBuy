package com.groupbuy.category.controller;

import com.groupbuy.category.dto.CategoryRequest;
import com.groupbuy.category.dto.CategoryResponse;
import com.groupbuy.category.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@Tag(name = "Categories", description = "Endpoints for browsing and managing product categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping("/api/v1/categories")
    @Operation(summary = "Get active categories", description = "Retrieves all active categories ordered by display order for public catalog navigation.")
    public ResponseEntity<List<CategoryResponse>> getActiveCategories() {
        return ResponseEntity.ok(categoryService.getActiveCategories());
    }

    @GetMapping("/api/v1/categories/{id}")
    @Operation(summary = "Get category by ID", description = "Retrieves category details by numeric ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Category retrieved"),
            @ApiResponse(responseCode = "404", description = "Category not found")
    })
    public ResponseEntity<CategoryResponse> getCategoryById(@PathVariable Long id) {
        return ResponseEntity.ok(categoryService.getCategoryById(id));
    }

    @GetMapping("/api/v1/categories/slug/{slug}")
    @Operation(summary = "Get category by slug", description = "Retrieves category details by unique slug.")
    public ResponseEntity<CategoryResponse> getCategoryBySlug(@PathVariable String slug) {
        return ResponseEntity.ok(categoryService.getCategoryBySlug(slug));
    }

    // Admin endpoints
    @GetMapping("/api/v1/admin/categories")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all categories (Admin)", description = "Returns all categories including inactive ones.", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<List<CategoryResponse>> getAllCategoriesAdmin() {
        return ResponseEntity.ok(categoryService.getAllCategories());
    }

    @PostMapping("/api/v1/admin/categories")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create category (Admin)", description = "Creates a new category.", security = @SecurityRequirement(name = "BearerAuth"))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Category created"),
            @ApiResponse(responseCode = "409", description = "Slug already exists")
    })
    public ResponseEntity<CategoryResponse> createCategory(@Valid @RequestBody CategoryRequest request) {
        CategoryResponse response = categoryService.createCategory(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/api/v1/admin/categories/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update category (Admin)", description = "Updates an existing category.", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<CategoryResponse> updateCategory(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.ok(categoryService.updateCategory(id, request));
    }

    @DeleteMapping("/api/v1/admin/categories/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete category (Admin)", description = "Deletes a category by ID.", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<Map<String, String>> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.ok(Map.of("message", "Category deleted successfully"));
    }
}

