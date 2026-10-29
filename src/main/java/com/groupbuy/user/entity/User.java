package com.groupbuy.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;

/**
 * JPA entity mapped to the existing {@code users} table defined in V1__initial_schema.sql.
 *
 * <p>Column mapping matches the V1 migration exactly:
 * id BIGSERIAL PK, email VARCHAR(255) UNIQUE, username VARCHAR(100) UNIQUE,
 * password_hash VARCHAR(255), full_name VARCHAR(150), role VARCHAR(50),
 * status VARCHAR(50), created_at TIMESTAMPTZ, updated_at TIMESTAMPTZ, version BIGINT.
 *
 * <p>Roles are stored as plain strings matching the CHECK constraint:
 * {@code role IN ('CUSTOMER','SELLER','ADMIN')}.
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 255)
    private String email;

    @Column(nullable = false, unique = true, length = 100)
    private String username;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Role role = Role.CUSTOMER;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private Status status = Status.ACTIVE;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    protected User() {
        // JPA default constructor
    }

    public User(String email, String username, String passwordHash, String fullName, Role role) {
        this.email = email;
        this.username = username;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.role = role;
        this.status = Status.ACTIVE;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    // ---- Getters ----

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getUsername() { return username; }
    public String getPasswordHash() { return passwordHash; }
    public String getFullName() { return fullName; }
    public Role getRole() { return role; }
    public Status getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Long getVersion() { return version; }

    // ---- Setters for mutable fields ----

    public void setEmail(String email) { this.email = email; }
    public void setUsername(String username) { this.username = username; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public void setRole(Role role) { this.role = role; }
    public void setStatus(Status status) { this.status = status; }

    /**
     * Roles matching the PostgreSQL CHECK constraint: role IN ('CUSTOMER','SELLER','ADMIN').
     */
    public enum Role {
        CUSTOMER,
        SELLER,
        ADMIN
    }

    /**
     * Account statuses matching the PostgreSQL CHECK constraint: status IN ('ACTIVE','SUSPENDED','DELETED').
     */
    public enum Status {
        ACTIVE,
        SUSPENDED,
        DELETED
    }
}
