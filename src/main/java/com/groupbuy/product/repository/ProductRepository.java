package com.groupbuy.product.repository;

import com.groupbuy.product.entity.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    Optional<Product> findBySku(String sku);

    boolean existsBySku(String sku);

    Page<Product> findByStatus(Product.Status status, Pageable pageable);

    Page<Product> findByCategoryIdAndStatus(Long categoryId, Product.Status status, Pageable pageable);

    Page<Product> findBySellerId(Long sellerId, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findByIdWithLock(@Param("id") Long id);

    @Query("SELECT p FROM Product p WHERE p.status = 'ACTIVE' AND " +
           "(CAST(:query AS string) IS NULL OR " +
           "LOWER(p.name) LIKE LOWER(CONCAT('%', CAST(:query AS string), '%')) OR " +
           "LOWER(p.description) LIKE LOWER(CONCAT('%', CAST(:query AS string), '%'))) AND " +
           "(:categoryId IS NULL OR p.category.id = :categoryId)")
    Page<Product> searchPublicProducts(@Param("query") String query, @Param("categoryId") Long categoryId, Pageable pageable);
}

