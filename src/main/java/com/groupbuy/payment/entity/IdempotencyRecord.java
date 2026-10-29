package com.groupbuy.payment.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "idempotency_records")
public class IdempotencyRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "idempotency_key", nullable = false, unique = true)
    private String idempotencyKey;

    @Column(name = "request_path", nullable = false)
    private String requestPath;

    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;

    @Column(name = "response_status")
    private Integer responseStatus;

    @Column(name = "response_body", columnDefinition = "TEXT")
    private String responseBody;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Status status = Status.PROCESSING;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    protected IdempotencyRecord() {
    }

    public IdempotencyRecord(String idempotencyKey, String requestPath, String requestHash, Instant expiresAt) {
        this.idempotencyKey = idempotencyKey;
        this.requestPath = requestPath;
        this.requestHash = requestHash;
        this.status = Status.PROCESSING;
        this.createdAt = Instant.now();
        this.expiresAt = expiresAt;
    }

    public void complete(int responseStatus, String responseBody) {
        this.status = Status.COMPLETED;
        this.responseStatus = responseStatus;
        this.responseBody = responseBody;
    }

    public void fail(int responseStatus, String responseBody) {
        this.status = Status.FAILED;
        this.responseStatus = responseStatus;
        this.responseBody = responseBody;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getRequestPath() { return requestPath; }
    public String getRequestHash() { return requestHash; }
    public Integer getResponseStatus() { return responseStatus; }
    public String getResponseBody() { return responseBody; }
    public Status getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getExpiresAt() { return expiresAt; }

    public enum Status {
        PROCESSING,
        COMPLETED,
        FAILED
    }
}

