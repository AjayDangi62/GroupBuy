package com.groupbuy.payment.entity;

import com.groupbuy.common.constants.CurrencyConstants;
import com.groupbuy.order.entity.Order;
import com.groupbuy.participation.entity.Participation;
import com.groupbuy.user.entity.User;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "payments")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "participation_id")
    private Participation participation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 3)
    private String currency = CurrencyConstants.DEFAULT_CURRENCY;

    @Column(name = "authorized_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal authorizedAmount;

    @Column(name = "captured_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal capturedAmount = BigDecimal.ZERO;

    @Column(name = "refunded_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal refundedAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Status status = Status.PENDING;

    @Column(name = "payment_gateway", nullable = false, length = 50)
    private String paymentGateway;

    @Column(name = "transaction_id")
    private String transactionId;

    @Column(name = "idempotency_key")
    private String idempotencyKey;

    @OneToMany(mappedBy = "payment", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<PaymentAttempt> attempts = new ArrayList<>();

    @OneToMany(mappedBy = "payment", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<PaymentEvent> events = new ArrayList<>();

    @OneToMany(mappedBy = "payment", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Refund> refunds = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    protected Payment() {
    }

    public Payment(User user, BigDecimal authorizedAmount, String currency, String paymentGateway, String idempotencyKey) {
        this.user = user;
        this.authorizedAmount = authorizedAmount != null
                ? authorizedAmount.setScale(CurrencyConstants.MONETARY_SCALE, CurrencyConstants.DEFAULT_ROUNDING_MODE)
                : BigDecimal.ZERO;
        this.currency = (currency == null || currency.isBlank()) ? CurrencyConstants.DEFAULT_CURRENCY : currency.trim().toUpperCase();
        this.paymentGateway = paymentGateway;
        this.idempotencyKey = idempotencyKey;
        this.status = Status.PENDING;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public void addAttempt(PaymentAttempt attempt) {
        attempts.add(attempt);
        attempt.setPayment(this);
    }

    public void addEvent(PaymentEvent event) {
        events.add(event);
        event.setPayment(this);
    }

    public void addRefund(Refund refund) {
        refunds.add(refund);
        refund.setPayment(this);
    }

    // Getters and Setters
    public Long getId() { return id; }
    public Participation getParticipation() { return participation; }
    public void setParticipation(Participation participation) { this.participation = participation; }
    public Order getOrder() { return order; }
    public void setOrder(Order order) { this.order = order; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public BigDecimal getAuthorizedAmount() { return authorizedAmount; }
    public void setAuthorizedAmount(BigDecimal authorizedAmount) {
        this.authorizedAmount = authorizedAmount != null ? authorizedAmount.setScale(CurrencyConstants.MONETARY_SCALE, CurrencyConstants.DEFAULT_ROUNDING_MODE) : BigDecimal.ZERO;
    }
    public BigDecimal getCapturedAmount() { return capturedAmount; }
    public void setCapturedAmount(BigDecimal capturedAmount) {
        this.capturedAmount = capturedAmount != null ? capturedAmount.setScale(CurrencyConstants.MONETARY_SCALE, CurrencyConstants.DEFAULT_ROUNDING_MODE) : BigDecimal.ZERO;
    }
    public BigDecimal getRefundedAmount() { return refundedAmount; }
    public void setRefundedAmount(BigDecimal refundedAmount) {
        this.refundedAmount = refundedAmount != null ? refundedAmount.setScale(CurrencyConstants.MONETARY_SCALE, CurrencyConstants.DEFAULT_ROUNDING_MODE) : BigDecimal.ZERO;
    }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public String getPaymentGateway() { return paymentGateway; }
    public void setPaymentGateway(String paymentGateway) { this.paymentGateway = paymentGateway; }
    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public List<PaymentAttempt> getAttempts() { return attempts; }
    public List<PaymentEvent> getEvents() { return events; }
    public List<Refund> getRefunds() { return refunds; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Long getVersion() { return version; }

    public enum Status {
        PENDING,
        AUTHORIZED,
        CAPTURED,
        PARTIALLY_REFUNDED,
        REFUNDED,
        FAILED
    }
}

