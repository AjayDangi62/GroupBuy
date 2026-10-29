package com.groupbuy.product.service;

import com.groupbuy.auth.security.UserPrincipal;
import com.groupbuy.category.entity.Category;
import com.groupbuy.category.repository.CategoryRepository;
import com.groupbuy.common.constants.CurrencyConstants;
import com.groupbuy.common.exception.BusinessException;
import com.groupbuy.common.exception.DuplicateResourceException;
import com.groupbuy.common.exception.ResourceNotFoundException;
import com.groupbuy.product.dto.ProductCreateRequest;
import com.groupbuy.product.dto.ProductResponse;
import com.groupbuy.product.dto.ProductUpdateRequest;
import com.groupbuy.product.entity.Product;
import com.groupbuy.product.repository.ProductRepository;
import com.groupbuy.user.entity.User;
import com.groupbuy.user.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    public ProductService(ProductRepository productRepository, CategoryRepository categoryRepository, UserRepository userRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        return ProductResponse.fromEntity(product);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> getPublicProducts(String query, Long categoryId, Pageable pageable) {
        String normalizedQuery = (query != null && !query.trim().isEmpty()) ? query.trim() : null;
        return productRepository.searchPublicProducts(normalizedQuery, categoryId, pageable)
                .map(ProductResponse::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> getSellerProducts(Long sellerId, Pageable pageable) {
        return productRepository.findBySellerId(sellerId, pageable)
                .map(ProductResponse::fromEntity);
    }

    @Transactional
    public ProductResponse createProduct(ProductCreateRequest request, UserPrincipal principal) {
        String normalizedSku = request.sku().trim().toUpperCase();
        if (productRepository.existsBySku(normalizedSku)) {
            throw new DuplicateResourceException("Product with SKU already exists: " + normalizedSku, "SKU_ALREADY_EXISTS");
        }

        if (request.basePrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Base price must be greater than zero", "INVALID_PRODUCT_PRICE");
        }

        if (request.totalInventory() < 0) {
            throw new BusinessException("Total inventory cannot be negative", "INVALID_INVENTORY");
        }

        User seller = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + principal.getId()));

        Category category = null;
        if (request.categoryId() != null) {
            category = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + request.categoryId()));
        }

        String currency = (request.currency() != null && !request.currency().isBlank())
                ? request.currency().trim().toUpperCase()
                : CurrencyConstants.DEFAULT_CURRENCY;

        Product.Status initialStatus = request.totalInventory() == 0 ? Product.Status.OUT_OF_STOCK : Product.Status.ACTIVE;

        Product product = new Product(
                seller,
                category,
                normalizedSku,
                request.name().trim(),
                request.description(),
                request.basePrice(),
                currency,
                request.totalInventory(),
                request.totalInventory(),
                initialStatus,
                request.imageUrl()
        );

        Product saved = productRepository.save(product);
        return ProductResponse.fromEntity(saved);
    }

    @Transactional
    public ProductResponse updateProduct(Long id, ProductUpdateRequest request, UserPrincipal principal) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        verifyOwnershipOrAdmin(product, principal);

        if (request.name() != null && !request.name().isBlank()) {
            product.setName(request.name().trim());
        }
        if (request.description() != null) {
            product.setDescription(request.description());
        }
        if (request.basePrice() != null) {
            if (request.basePrice().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException("Base price must be greater than zero", "INVALID_PRODUCT_PRICE");
            }
            product.setBasePrice(request.basePrice());
        }
        if (request.categoryId() != null) {
            Category category = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + request.categoryId()));
            product.setCategory(category);
        }
        if (request.status() != null) {
            product.setStatus(request.status());
        }
        if (request.imageUrl() != null) {
            product.setImageUrl(request.imageUrl());
        }

        if (request.totalInventory() != null || request.availableInventory() != null) {
            int newTotal = request.totalInventory() != null ? request.totalInventory() : product.getTotalInventory();
            int newAvailable = request.availableInventory() != null ? request.availableInventory() : product.getAvailableInventory();
            product.updateInventory(newTotal, newAvailable);
        }

        Product saved = productRepository.save(product);
        return ProductResponse.fromEntity(saved);
    }

    @Transactional
    public void deleteProduct(Long id, UserPrincipal principal) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        verifyOwnershipOrAdmin(product, principal);

        // Soft delete / archive to preserve historical relationships in orders/campaigns
        product.setStatus(Product.Status.ARCHIVED);
        productRepository.save(product);
    }

    private void verifyOwnershipOrAdmin(Product product, UserPrincipal principal) {
        boolean isAdmin = principal != null && principal.getAuthorities() != null && principal.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isOwner = product != null && product.getSeller() != null
                && product.getSeller().getId() != null
                && principal != null && principal.getId() != null
                && product.getSeller().getId().equals(principal.getId());

        if (!isAdmin && !isOwner) {
            throw new AccessDeniedException("You do not have permission to modify this product");
        }
    }
}

