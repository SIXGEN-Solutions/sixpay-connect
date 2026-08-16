package com.sixpay.security.infrastructure.authentication.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "security_password_history")
public class PasswordHistoryJpaEntity {
    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected PasswordHistoryJpaEntity() {}

    public static PasswordHistoryJpaEntity archived(
            UUID userId,
            String passwordHash,
            Instant createdAt
    ) {
        PasswordHistoryJpaEntity entity = new PasswordHistoryJpaEntity();
        entity.id = UUID.randomUUID();
        entity.userId = Objects.requireNonNull(userId);
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException("Password-history hash must not be blank");
        }
        entity.passwordHash = passwordHash;
        entity.createdAt = Objects.requireNonNull(createdAt);
        return entity;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getPasswordHash() { return passwordHash; }
    public Instant getCreatedAt() { return createdAt; }
}
