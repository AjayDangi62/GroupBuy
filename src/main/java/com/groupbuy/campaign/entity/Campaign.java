package com.groupbuy.campaign.entity;

import com.groupbuy.common.constants.CurrencyConstants;
import com.groupbuy.common.exception.BusinessException;
import com.groupbuy.pricing.domain.PricingTier;
import com.groupbuy.product.entity.Product;
import com.groupbuy.user.entity.User;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * JPA entity mapped to the campaigns table.
 */
@Entity
@Table(name = "campaigns")
public class Campaign {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seller_id", nullable = false)
    private User seller;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "target_min_quantity", nullable = false)
    private int targetMinQuantity;

    @Column(name = "max_campaign_quantity")
    private Integer maxCampaignQuantity;

    @Column(name = "current_paid_units", nullable = false)
    private int currentPaidUnits = 0;

    @Column(name = "min_participants", nullable = false)
    private int minParticipants = 1;

    @Column(name = "current_participants", nullable = false)
    private int currentParticipants = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Status status = Status.DRAFT;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at", nullable = false)
    private Instant endsAt;

    @Column(name = "finalized_at")
    private Instant finalizedAt;

    @Column(name = "final_unit_price", precision = 12, scale = 2)
    private BigDecimal finalUnitPrice;

    @Column(nullable = false, length = 3)
    private String currency = CurrencyConstants.DEFAULT_CURRENCY;

    @OneToMany(mappedBy = "campaign", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("minQuantity ASC")
    private List<CampaignPricingTier> pricingTiers = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    protected Campaign() {
    }

    public Campaign(Product product, User seller, String title, String description,
                    int targetMinQuantity, Integer maxCampaignQuantity, int minParticipants,
                    Instant startsAt, Instant endsAt, String currency) {
        this.product = product;
        this.seller = seller;
        this.title = title;
        this.description = description;
        this.targetMinQuantity = targetMinQuantity;
        this.maxCampaignQuantity = maxCampaignQuantity;
        this.minParticipants = Math.max(1, minParticipants);
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.currency = (currency == null || currency.isBlank()) ? CurrencyConstants.DEFAULT_CURRENCY : currency.trim().toUpperCase();
        this.status = Instant.now().isBefore(startsAt) ? Status.SCHEDULED : Status.ACTIVE;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public void addPricingTier(CampaignPricingTier tier) {
        pricingTiers.add(tier);
        tier.setCampaign(this);
    }

    public List<PricingTier> toDomainTiers() {
        return pricingTiers.stream().map(CampaignPricingTier::toDomain).toList();
    }

    public boolean isJoinable() {
        Instant now = Instant.now();
        return this.status == Status.ACTIVE &&
                !now.isBefore(this.startsAt) &&
                now.isBefore(this.endsAt) &&
                (this.maxCampaignQuantity == null || this.currentPaidUnits < this.maxCampaignQuantity);
    }

    public void incrementParticipation(int units) {
        if (!isJoinable()) {
            throw new BusinessException("Campaign is not currently active for joining", "CAMPAIGN_NOT_ACTIVE");
        }
        if (maxCampaignQuantity != null && (currentPaidUnits + units) > maxCampaignQuantity) {
            throw new BusinessException("Requested units would exceed maximum campaign limit of " + maxCampaignQuantity, "CAMPAIGN_QUANTITY_EXCEEDED");
        }
        this.currentPaidUnits += units;
        this.currentParticipants += 1;
    }

    public void decrementParticipation(int units) {
        this.currentPaidUnits = Math.max(0, this.currentPaidUnits - units);
        this.currentParticipants = Math.max(0, this.currentParticipants - 1);
    }

    // Getters and Setters
    public Long getId() { return id; }
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
    public User getSeller() { return seller; }
    public void setSeller(User seller) { this.seller = seller; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public int getTargetMinQuantity() { return targetMinQuantity; }
    public void setTargetMinQuantity(int targetMinQuantity) { this.targetMinQuantity = targetMinQuantity; }
    public Integer getMaxCampaignQuantity() { return maxCampaignQuantity; }
    public void setMaxCampaignQuantity(Integer maxCampaignQuantity) { this.maxCampaignQuantity = maxCampaignQuantity; }
    public int getCurrentPaidUnits() { return currentPaidUnits; }
    public void setCurrentPaidUnits(int currentPaidUnits) { this.currentPaidUnits = currentPaidUnits; }
    public int getMinParticipants() { return minParticipants; }
    public void setMinParticipants(int minParticipants) { this.minParticipants = minParticipants; }
    public int getCurrentParticipants() { return currentParticipants; }
    public void setCurrentParticipants(int currentParticipants) { this.currentParticipants = currentParticipants; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public Instant getStartsAt() { return startsAt; }
    public void setStartsAt(Instant startsAt) { this.startsAt = startsAt; }
    public Instant getEndsAt() { return endsAt; }
    public void setEndsAt(Instant endsAt) { this.endsAt = endsAt; }
    public Instant getFinalizedAt() { return finalizedAt; }
    public void setFinalizedAt(Instant finalizedAt) { this.finalizedAt = finalizedAt; }
    public BigDecimal getFinalUnitPrice() { return finalUnitPrice; }
    public void setFinalUnitPrice(BigDecimal finalUnitPrice) {
        this.finalUnitPrice = finalUnitPrice != null ? finalUnitPrice.setScale(CurrencyConstants.MONETARY_SCALE, CurrencyConstants.DEFAULT_ROUNDING_MODE) : null;
    }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public List<CampaignPricingTier> getPricingTiers() { return pricingTiers; }
    public void setPricingTiers(List<CampaignPricingTier> pricingTiers) { this.pricingTiers = pricingTiers; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Long getVersion() { return version; }

    public enum Status {
        DRAFT,
        SCHEDULED,
        ACTIVE,
        ENDED,
        FINALIZING,
        FINALIZED,
        SUCCESSFUL,
        FAILED,
        EXPIRED,
        CANCELLED
    }
}

