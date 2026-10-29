package com.groupbuy.product.entity;

import com.groupbuy.category.entity.Category;
import com.groupbuy.common.constants.CurrencyConstants;
import com.groupbuy.user.entity.User;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * JPA entity mapped to the products table.
 */
@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(nullable = false, unique = true, length = 100)
    private String sku;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "base_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal basePrice;

    @Column(nullable = false, length = 3)
    private String currency = CurrencyConstants.DEFAULT_CURRENCY;

    @Column(name = "total_inventory", nullable = false)
    private int totalInventory;

    @Column(name = "available_inventory", nullable = false)
    private int availableInventory;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Status status = Status.ACTIVE;

    @Column(name = "image_url", length = 512)
    private String imageUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    protected Product() {
    }

    public Product(User seller, Category category, String sku, String name, String description,
                   BigDecimal basePrice, String currency, int totalInventory, int availableInventory,
                   Status status, String imageUrl) {
        this.seller = seller;
        this.category = category;
        this.sku = sku;
        this.name = name;
        this.description = description;
        this.basePrice = basePrice != null ? basePrice.setScale(CurrencyConstants.MONETARY_SCALE, CurrencyConstants.DEFAULT_ROUNDING_MODE) : BigDecimal.ZERO;
        this.currency = (currency == null || currency.isBlank()) ? CurrencyConstants.DEFAULT_CURRENCY : currency.trim().toUpperCase();
        this.totalInventory = totalInventory;
        this.availableInventory = availableInventory;
        this.status = status != null ? status : Status.ACTIVE;
        this.imageUrl = imageUrl;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // Domain methods for stock management
    public void deductStock(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity to deduct must be positive");
        }
        if (this.availableInventory < quantity) {
            throw new IllegalStateException("Insufficient inventory: requested " + quantity + " but only " + this.availableInventory + " available");
        }
        this.availableInventory -= quantity;
        if (this.availableInventory == 0) {
            this.status = Status.OUT_OF_STOCK;
        }
    }

    public void restoreStock(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity to restore must be positive");
        }
        this.availableInventory = Math.min(this.totalInventory, this.availableInventory + quantity);
        if (this.availableInventory > 0 && this.status == Status.OUT_OF_STOCK) {
            this.status = Status.ACTIVE;
        }
    }

    public void updateInventory(int newTotal, int newAvailable) {
        if (newTotal < 0 || newAvailable < 0 || newAvailable > newTotal) {
            throw new IllegalArgumentException("Invalid inventory parameters: total=" + newTotal + ", available=" + newAvailable);
        }
        this.totalInventory = newTotal;
        this.availableInventory = newAvailable;
        if (this.availableInventory == 0 && this.status == Status.ACTIVE) {
            this.status = Status.OUT_OF_STOCK;
        } else if (this.availableInventory > 0 && this.status == Status.OUT_OF_STOCK) {
            this.status = Status.ACTIVE;
        }
    }

    // Getters and Setters
    public Long getId() { return id; }
    public User getSeller() { return seller; }
    public void setSeller(User seller) { this.seller = seller; }
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getBasePrice() { return basePrice; }
    public void setBasePrice(BigDecimal basePrice) {
        this.basePrice = basePrice != null ? basePrice.setScale(CurrencyConstants.MONETARY_SCALE, CurrencyConstants.DEFAULT_ROUNDING_MODE) : BigDecimal.ZERO;
    }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public int getTotalInventory() { return totalInventory; }
    public void setTotalInventory(int totalInventory) { this.totalInventory = totalInventory; }
    public int getAvailableInventory() { return availableInventory; }
    public void setAvailableInventory(int availableInventory) { this.availableInventory = availableInventory; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Long getVersion() { return version; }

    public enum Status {
        ACTIVE,
        ARCHIVED,
        OUT_OF_STOCK
    }
}

