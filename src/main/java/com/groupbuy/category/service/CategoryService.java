package com.groupbuy.category.service;

import com.groupbuy.category.dto.CategoryRequest;
import com.groupbuy.category.dto.CategoryResponse;
import com.groupbuy.category.entity.Category;
import com.groupbuy.category.repository.CategoryRepository;
import com.groupbuy.common.exception.DuplicateResourceException;
import com.groupbuy.common.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getActiveCategories() {
        return categoryRepository.findByActiveTrueOrderByDisplayOrderAsc()
                .stream()
                .map(CategoryResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAll()
                .stream()
                .map(CategoryResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
        return CategoryResponse.fromEntity(category);
    }

    @Transactional(readOnly = true)
    public CategoryResponse getCategoryBySlug(String slug) {
        Category category = categoryRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with slug: " + slug));
        return CategoryResponse.fromEntity(category);
    }

    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        String normalizedSlug = request.slug().trim().toLowerCase();
        if (categoryRepository.existsBySlug(normalizedSlug)) {
            throw new DuplicateResourceException("Category slug already exists: " + normalizedSlug, "CATEGORY_SLUG_EXISTS");
        }

        Category parent = null;
        if (request.parentId() != null) {
            parent = categoryRepository.findById(request.parentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent category not found with id: " + request.parentId()));
        }

        Category category = new Category(
                request.name().trim(),
                normalizedSlug,
                request.description(),
                parent,
                request.active() != null ? request.active() : true,
                request.displayOrder() != null ? request.displayOrder() : 0
        );

        Category saved = categoryRepository.save(category);
        return CategoryResponse.fromEntity(saved);
    }

    @Transactional
    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));

        String normalizedSlug = request.slug().trim().toLowerCase();
        if (!category.getSlug().equalsIgnoreCase(normalizedSlug) && categoryRepository.existsBySlug(normalizedSlug)) {
            throw new DuplicateResourceException("Category slug already exists: " + normalizedSlug, "CATEGORY_SLUG_EXISTS");
        }

        Category parent = null;
        if (request.parentId() != null) {
            if (request.parentId().equals(id)) {
                throw new IllegalArgumentException("A category cannot be its own parent");
            }
            parent = categoryRepository.findById(request.parentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent category not found with id: " + request.parentId()));
        }

        category.setName(request.name().trim());
        category.setSlug(normalizedSlug);
        category.setDescription(request.description());
        category.setParent(parent);
        if (request.active() != null) {
            category.setActive(request.active());
        }
        if (request.displayOrder() != null) {
            category.setDisplayOrder(request.displayOrder());
        }

        Category updated = categoryRepository.save(category);
        return CategoryResponse.fromEntity(updated);
    }

    @Transactional
    public void deleteCategory(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new ResourceNotFoundException("Category not found with id: " + id);
        }
        categoryRepository.deleteById(id);
    }
}

