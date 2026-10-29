package com.groupbuy.auth.entity;

import com.groupbuy.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * JPA entity mapped to the {@code refresh_tokens} table.
 *
 * <p>V1 columns: id, user_id (FK users), token_hash (UNIQUE), revoked (BOOLEAN),
 * expires_at (TIMESTAMPTZ), created_at (TIMESTAMPTZ).
 * <p>V2 additions: family_id (VARCHAR(64)), replaced_by_id (FK self).
 *
 * <p>Only a SHA-256 hash of the actual token value is stored. The plaintext token
 * is returned to the client exactly once and never persisted.
 */
@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "token_hash", nullable = false, unique = true, length = 255)
    private String tokenHash;

    @Column(nullable = false)
    private boolean revoked = false;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    /**
     * Groups tokens into a rotation family. All tokens in a family originate
     * from the same login event. On reuse detection the entire family is revoked.
     */
    @Column(name = "family_id", nullable = false, length = 64)
    private String familyId;

    /**
     * Points to the successor token that replaced this one during rotation.
     * Null if this token has not been rotated yet.
     */
    @Column(name = "replaced_by_id")
    private Long replacedById;

    protected RefreshToken() {
        // JPA default constructor
    }

    public RefreshToken(User user, String tokenHash, Instant expiresAt, String familyId) {
        this.user = user;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.familyId = familyId;
        this.revoked = false;
        this.createdAt = Instant.now();
    }

    // ---- Getters ----

    public Long getId() { return id; }
    public User getUser() { return user; }
    public String getTokenHash() { return tokenHash; }
    public boolean isRevoked() { return revoked; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getCreatedAt() { return createdAt; }
    public String getFamilyId() { return familyId; }
    public Long getReplacedById() { return replacedById; }

    // ---- Setters for mutable fields ----

    public void setRevoked(boolean revoked) { this.revoked = revoked; }
    public void setReplacedById(Long replacedById) { this.replacedById = replacedById; }

    public boolean isExpired() {
        return Instant.now().isAfter(this.expiresAt);
    }

    public boolean isUsable() {
        return !isRevoked() && !isExpired();
    }
}
