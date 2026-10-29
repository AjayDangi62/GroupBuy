package com.groupbuy.campaign.entity;

import com.groupbuy.common.constants.CurrencyConstants;
import com.groupbuy.pricing.domain.PricingTier;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "pricing_tiers")
public class CampaignPricingTier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    @Column(name = "min_quantity", nullable = false)
    private int minQuantity;

    @Column(name = "max_quantity")
    private Integer maxQuantity;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false, length = 3)
    private String currency = CurrencyConstants.DEFAULT_CURRENCY;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected CampaignPricingTier() {
    }

    public CampaignPricingTier(Campaign campaign, int minQuantity, Integer maxQuantity, BigDecimal unitPrice, String currency) {
        this.campaign = campaign;
        this.minQuantity = minQuantity;
        this.maxQuantity = maxQuantity;
        this.unitPrice = unitPrice != null ? unitPrice.setScale(CurrencyConstants.MONETARY_SCALE, CurrencyConstants.DEFAULT_ROUNDING_MODE) : BigDecimal.ZERO;
        this.currency = (currency == null || currency.isBlank()) ? CurrencyConstants.DEFAULT_CURRENCY : currency.trim().toUpperCase();
        this.createdAt = Instant.now();
    }

    public PricingTier toDomain() {
        return new PricingTier(minQuantity, maxQuantity, unitPrice, currency);
    }

    // Getters and Setters
    public Long getId() { return id; }
    public Campaign getCampaign() { return campaign; }
    public void setCampaign(Campaign campaign) { this.campaign = campaign; }
    public int getMinQuantity() { return minQuantity; }
    public void setMinQuantity(int minQuantity) { this.minQuantity = minQuantity; }
    public Integer getMaxQuantity() { return maxQuantity; }
    public void setMaxQuantity(Integer maxQuantity) { this.maxQuantity = maxQuantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice != null ? unitPrice.setScale(CurrencyConstants.MONETARY_SCALE, CurrencyConstants.DEFAULT_ROUNDING_MODE) : BigDecimal.ZERO;
    }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public Instant getCreatedAt() { return createdAt; }
}

