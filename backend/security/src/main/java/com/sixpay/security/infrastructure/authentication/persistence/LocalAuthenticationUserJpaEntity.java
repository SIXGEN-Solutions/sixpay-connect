package com.sixpay.security.infrastructure.authentication.persistence;

import com.sixpay.security.domain.authentication.LocalAuthenticationAccountStatus;
import com.sixpay.security.infrastructure.authentication.identity.SecurityUserAccountJpaEntity;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "security_local_users")
public class LocalAuthenticationUserJpaEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private SecurityUserAccountJpaEntity userAccount;

    @Column(nullable = false, unique = true, length = 150)
    private String subject;

    @Column(nullable = false, length = 150)
    private String username;

    @Column(
            name = "normalized_username",
            nullable = false,
            unique = true,
            length = 150
    )
    private String normalizedUsername;

    @Column(
            name = "password_hash",
            nullable = false,
            length = 100
    )
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private LocalAuthenticationAccountStatus status;

    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "last_authenticated_at")
    private Instant lastAuthenticatedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private long version;

    protected LocalAuthenticationUserJpaEntity() {
    }

    public UUID getId() {
        return id;
    }

    public SecurityUserAccountJpaEntity getUserAccount() {
        return userAccount;
    }

    public String getSubject() {
        return subject;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public LocalAuthenticationAccountStatus getStatus() {
        return status;
    }

    public int getFailedAttempts() {
        return failedAttempts;
    }

    public Instant getLockedUntil() {
        return lockedUntil;
    }

    public Instant getLastAuthenticatedAt() {
        return lastAuthenticatedAt;
    }

    public void updateAuthenticationState(
            int failedAttempts,
            Instant lockedUntil,
            Instant lastAuthenticatedAt,
            Instant updatedAt
    ) {
        this.failedAttempts = failedAttempts;
        this.lockedUntil = lockedUntil;
        this.lastAuthenticatedAt = lastAuthenticatedAt;
        this.updatedAt = updatedAt;
    }
}
