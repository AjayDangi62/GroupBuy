package com.groupbuy.participation.entity;

import com.groupbuy.campaign.entity.Campaign;
import com.groupbuy.common.constants.CurrencyConstants;
import com.groupbuy.user.entity.User;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "participations")
public class Participation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "authorized_unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal authorizedUnitPrice;

    @Column(name = "authorized_total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal authorizedTotalAmount;

    @Column(name = "final_unit_price", precision = 12, scale = 2)
    private BigDecimal finalUnitPrice;

    @Column(name = "final_total_amount", precision = 12, scale = 2)
    private BigDecimal finalTotalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Status status = Status.INITIATED;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    protected Participation() {
    }

    public Participation(Campaign campaign, User user, int quantity, BigDecimal authorizedUnitPrice) {
        this.campaign = campaign;
        this.user = user;
        this.quantity = quantity;
        this.authorizedUnitPrice = authorizedUnitPrice != null
                ? authorizedUnitPrice.setScale(CurrencyConstants.MONETARY_SCALE, CurrencyConstants.DEFAULT_ROUNDING_MODE)
                : BigDecimal.ZERO;
        this.authorizedTotalAmount = this.authorizedUnitPrice.multiply(BigDecimal.valueOf(quantity))
                .setScale(CurrencyConstants.MONETARY_SCALE, CurrencyConstants.DEFAULT_ROUNDING_MODE);
        this.status = Status.INITIATED;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public void settleFinalPrice(BigDecimal settledUnitPrice) {
        this.finalUnitPrice = settledUnitPrice.setScale(CurrencyConstants.MONETARY_SCALE, CurrencyConstants.DEFAULT_ROUNDING_MODE);
        this.finalTotalAmount = this.finalUnitPrice.multiply(BigDecimal.valueOf(this.quantity))
                .setScale(CurrencyConstants.MONETARY_SCALE, CurrencyConstants.DEFAULT_ROUNDING_MODE);
    }

    // Getters and Setters
    public Long getId() { return id; }
    public Campaign getCampaign() { return campaign; }
    public void setCampaign(Campaign campaign) { this.campaign = campaign; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public BigDecimal getAuthorizedUnitPrice() { return authorizedUnitPrice; }
    public void setAuthorizedUnitPrice(BigDecimal authorizedUnitPrice) {
        this.authorizedUnitPrice = authorizedUnitPrice != null ? authorizedUnitPrice.setScale(CurrencyConstants.MONETARY_SCALE, CurrencyConstants.DEFAULT_ROUNDING_MODE) : BigDecimal.ZERO;
    }
    public BigDecimal getAuthorizedTotalAmount() { return authorizedTotalAmount; }
    public void setAuthorizedTotalAmount(BigDecimal authorizedTotalAmount) {
        this.authorizedTotalAmount = authorizedTotalAmount != null ? authorizedTotalAmount.setScale(CurrencyConstants.MONETARY_SCALE, CurrencyConstants.DEFAULT_ROUNDING_MODE) : BigDecimal.ZERO;
    }
    public BigDecimal getFinalUnitPrice() { return finalUnitPrice; }
    public void setFinalUnitPrice(BigDecimal finalUnitPrice) { this.finalUnitPrice = finalUnitPrice; }
    public BigDecimal getFinalTotalAmount() { return finalTotalAmount; }
    public void setFinalTotalAmount(BigDecimal finalTotalAmount) { this.finalTotalAmount = finalTotalAmount; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Long getVersion() { return version; }

    public enum Status {
        INITIATED,
        AUTHORIZED,
        CONFIRMED,
        CANCELLED,
        REFUNDED
    }
}

