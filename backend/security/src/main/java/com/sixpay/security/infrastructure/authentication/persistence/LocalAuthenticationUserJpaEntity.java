package com.sixpay.security.infrastructure.authentication.persistence;

import com.sixpay.security.domain.authentication.LocalAuthenticationAccountStatus;
import com.sixpay.security.domain.authentication.PasswordPolicy;
import com.sixpay.security.infrastructure.authentication.identity.SecurityUserAccountJpaEntity;
import jakarta.persistence.*;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
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

    @Column(name = "normalized_username", nullable = false, unique = true, length = 150)
    private String normalizedUsername;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "must_change_password", nullable = false)
    private boolean mustChangePassword;

    @Column(name = "password_changed_at")
    private Instant passwordChangedAt;

    @Column(name = "password_expires_at")
    private Instant expiresAt;

    @Column(name = "credential_updated_at", nullable = false)
    private Instant credentialUpdatedAt;

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

    public static LocalAuthenticationUserJpaEntity provisioned(
            SecurityUserAccountJpaEntity account,
            String passwordHash,
            Instant now
    ) {
        LocalAuthenticationUserJpaEntity entity = new LocalAuthenticationUserJpaEntity();
        entity.id = UUID.randomUUID();
        entity.userAccount = account;
        entity.subject = account.getId().toString();
        entity.username = account.getUsername();
        entity.normalizedUsername = normalize(account.getUsername());
        entity.passwordHash = passwordHash;
        entity.mustChangePassword = true;
        entity.passwordChangedAt = null;
        entity.expiresAt = null;
        entity.credentialUpdatedAt = now;
        entity.status = LocalAuthenticationAccountStatus.ACTIVE;
        entity.failedAttempts = 0;
        entity.createdAt = now;
        entity.updatedAt = now;
        return entity;
    }

    public UUID getId() { return id; }
    public SecurityUserAccountJpaEntity getUserAccount() { return userAccount; }
    public String getSubject() { return subject; }
    public String getPasswordHash() { return passwordHash; }
    public boolean isMustChangePassword() { return mustChangePassword; }
    public Instant getPasswordChangedAt() { return passwordChangedAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getCredentialUpdatedAt() { return credentialUpdatedAt; }
    public LocalAuthenticationAccountStatus getStatus() { return status; }
    public int getFailedAttempts() { return failedAttempts; }
    public Instant getLockedUntil() { return lockedUntil; }
    public Instant getLastAuthenticatedAt() { return lastAuthenticatedAt; }

    public void rename(String username, Instant now) {
        this.username = username;
        this.normalizedUsername = normalize(username);
        this.updatedAt = now;
    }

    public void setEnabled(boolean enabled, Instant now) {
        this.status = enabled ? LocalAuthenticationAccountStatus.ACTIVE : LocalAuthenticationAccountStatus.DISABLED;
        this.updatedAt = now;
    }

    public void resetPassword(String bcryptHash, Instant now) {
        this.passwordHash = bcryptHash;
        this.mustChangePassword = true;
        this.passwordChangedAt = null;
        this.expiresAt = null;
        this.credentialUpdatedAt = now;
        this.failedAttempts = 0;
        this.lockedUntil = null;
        this.updatedAt = now;
    }

    public void changePassword(String bcryptHash, Instant now, PasswordPolicy policy) {
        this.passwordHash = bcryptHash;
        this.mustChangePassword = false;
        this.passwordChangedAt = now;
        this.expiresAt = now.plus(Duration.ofDays(policy.expirationDays()));
        this.credentialUpdatedAt = now;
        this.failedAttempts = 0;
        this.lockedUntil = null;
        this.updatedAt = now;
    }

    public void updateAuthenticationState(int failedAttempts, Instant lockedUntil, Instant lastAuthenticatedAt, Instant updatedAt) {
        this.failedAttempts = failedAttempts;
        this.lockedUntil = lockedUntil;
        this.lastAuthenticatedAt = lastAuthenticatedAt;
        this.updatedAt = updatedAt;
    }

    private static String normalize(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }
}
