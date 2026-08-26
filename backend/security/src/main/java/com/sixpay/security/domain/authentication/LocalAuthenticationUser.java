package com.sixpay.security.domain.authentication;

import com.sixpay.common.validation.Preconditions;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public record LocalAuthenticationUser(
        UUID id,
        UUID userId,
        String subject,
        String username,
        LocalCredential credential,
        LocalAuthenticationAccountStatus status,
        SixpayUserAccountStatus userAccountStatus,
        Set<String> authorities,
        int failedAttempts,
        Instant lockedUntil,
        Instant lastAuthenticatedAt
) {
    public LocalAuthenticationUser {
        id = Preconditions.requireNonNull(id, "Local authentication user id must not be null");
        userId = Preconditions.requireNonNull(userId, "SIXPAY user id must not be null");
        subject = Preconditions.requireNonBlank(subject, "Local authentication subject must not be blank");
        username = Preconditions.requireNonBlank(username, "Local authentication username must not be blank");
        credential = Preconditions.requireNonNull(credential, "Local credential must not be null");
        status = Preconditions.requireNonNull(status, "Local authentication status must not be null");
        userAccountStatus = Preconditions.requireNonNull(userAccountStatus, "SIXPAY user account status must not be null");
        authorities = Set.copyOf(Preconditions.requireNonNull(authorities, "Local authentication authorities must not be null"));
        if (!userId.equals(credential.userId())) {
            throw new IllegalArgumentException("Local credential must belong to the canonical SIXPAY user");
        }
        if (failedAttempts < 0) {
            throw new IllegalArgumentException("Failed attempts must not be negative");
        }
    }

    public String passwordHash() { return credential.passwordHash(); }
    public boolean mustChangePassword() { return credential.mustChangePassword(); }
    public Instant passwordChangedAt() { return credential.passwordChangedAt(); }
    public Instant expiresAt() { return credential.expiresAt(); }
    public Instant credentialUpdatedAt() { return credential.updatedAt(); }
    public boolean passwordExpiredAt(Instant now) { return credential.expiredAt(now); }
    public boolean passwordChangeRequiredAt(Instant now) { return credential.changeRequiredAt(now); }

    public LocalAuthenticationUser withUserChangedPassword(String newPasswordHash, Instant now, PasswordPolicy policy) {
        return withCredential(credential.changedByUser(newPasswordHash, now, policy));
    }

    public LocalAuthenticationUser withAdministrativelyResetPassword(String temporaryPasswordHash, Instant now) {
        return withCredential(credential.reset(temporaryPasswordHash, now));
    }

    public boolean active() {
        return status == LocalAuthenticationAccountStatus.ACTIVE && userAccountStatus == SixpayUserAccountStatus.ACTIVE;
    }

    public boolean lockedAt(Instant now) {
        Preconditions.requireNonNull(now, "Current time must not be null");
        return lockedUntil != null && now.isBefore(lockedUntil);
    }

    public LocalAuthenticationUser unlockIfExpired(Instant now) {
        Preconditions.requireNonNull(now, "Current time must not be null");
        if (lockedUntil == null || now.isBefore(lockedUntil)) return this;
        return withAuthenticationState(0, null, lastAuthenticatedAt);
    }

    public LocalAuthenticationUser authenticationSucceeded(Instant now) {
        Preconditions.requireNonNull(now, "Authentication time must not be null");
        return withAuthenticationState(0, null, now);
    }

    public LocalAuthenticationUser authenticationFailed(Instant now, int maximumFailedAttempts, Duration lockDuration) {
        Preconditions.requireNonNull(now, "Authentication time must not be null");
        Preconditions.requireNonNull(lockDuration, "Lock duration must not be null");
        if (maximumFailedAttempts < 1) throw new IllegalArgumentException("Maximum failed attempts must be positive");
        if (lockDuration.isZero() || lockDuration.isNegative()) throw new IllegalArgumentException("Lock duration must be positive");
        int nextFailedAttempts = failedAttempts + 1;
        Instant nextLockedUntil = nextFailedAttempts >= maximumFailedAttempts ? now.plus(lockDuration) : null;
        return withAuthenticationState(nextFailedAttempts, nextLockedUntil, lastAuthenticatedAt);
    }

    public String canonicalSubject() { return userId.toString(); }

    private LocalAuthenticationUser withAuthenticationState(int nextFailedAttempts, Instant nextLockedUntil, Instant nextLastAuthenticatedAt) {
        return new LocalAuthenticationUser(
                id, userId, subject, username, credential, status, userAccountStatus, authorities,
                nextFailedAttempts, nextLockedUntil, nextLastAuthenticatedAt
        );
    }

    private LocalAuthenticationUser withCredential(LocalCredential nextCredential) {
        Objects.requireNonNull(nextCredential, "Next local credential must not be null");
        return new LocalAuthenticationUser(
                id, userId, subject, username, nextCredential, status, userAccountStatus, authorities,
                0, null, lastAuthenticatedAt
        );
    }
}
