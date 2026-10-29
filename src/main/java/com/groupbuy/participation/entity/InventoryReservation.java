package com.groupbuy.participation.entity;

import com.groupbuy.campaign.entity.Campaign;
import com.groupbuy.product.entity.Product;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "inventory_reservations")
public class InventoryReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participation_id")
    private Participation participation;

    @Column(name = "reserved_quantity", nullable = false)
    private int reservedQuantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Status status = Status.PENDING;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    protected InventoryReservation() {
    }

    public InventoryReservation(Campaign campaign, Product product, Participation participation, int reservedQuantity, Instant expiresAt) {
        this.campaign = campaign;
        this.product = product;
        this.participation = participation;
        this.reservedQuantity = reservedQuantity;
        this.expiresAt = expiresAt;
        this.status = Status.PENDING;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public boolean isExpired() {
        return Instant.now().isAfter(this.expiresAt) && this.status == Status.PENDING;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public Campaign getCampaign() { return campaign; }
    public void setCampaign(Campaign campaign) { this.campaign = campaign; }
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
    public Participation getParticipation() { return participation; }
    public void setParticipation(Participation participation) { this.participation = participation; }
    public int getReservedQuantity() { return reservedQuantity; }
    public void setReservedQuantity(int reservedQuantity) { this.reservedQuantity = reservedQuantity; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Long getVersion() { return version; }

    public enum Status {
        PENDING,
        COMMITTED,
        RELEASED,
        EXPIRED
    }
}

