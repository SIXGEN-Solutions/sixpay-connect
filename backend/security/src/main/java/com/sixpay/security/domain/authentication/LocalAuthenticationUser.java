package com.sixpay.security.domain.authentication;

import com.sixpay.common.validation.Preconditions;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record LocalAuthenticationUser(
        UUID id,
        String subject,
        String username,
        String passwordHash,
        LocalAuthenticationAccountStatus status,
        Set<String> authorities,
        int failedAttempts,
        Instant lockedUntil,
        Instant lastAuthenticatedAt
) {

    public LocalAuthenticationUser {
        id = Preconditions.requireNonNull(id, "Local authentication user id must not be null");
        subject = Preconditions.requireNonBlank(subject, "Local authentication subject must not be blank");
        username = Preconditions.requireNonBlank(username, "Local authentication username must not be blank");
        passwordHash = Preconditions.requireNonBlank(passwordHash, "Local authentication password hash must not be blank");
        status = Preconditions.requireNonNull(status, "Local authentication status must not be null");
        authorities = Set.copyOf(
                Preconditions.requireNonNull(authorities, "Local authentication authorities must not be null")
        );

        if (failedAttempts < 0) {
            throw new IllegalArgumentException("Failed attempts must not be negative");
        }
    }

    public boolean active() {
        return status == LocalAuthenticationAccountStatus.ACTIVE;
    }

    public boolean lockedAt(Instant now) {
        Preconditions.requireNonNull(now, "Current time must not be null");
        return lockedUntil != null && now.isBefore(lockedUntil);
    }

    public LocalAuthenticationUser unlockIfExpired(Instant now) {
        Preconditions.requireNonNull(now, "Current time must not be null");

        if (lockedUntil == null || now.isBefore(lockedUntil)) {
            return this;
        }

        return withAuthenticationState(0, null, lastAuthenticatedAt);
    }

    public LocalAuthenticationUser authenticationSucceeded(Instant now) {
        Preconditions.requireNonNull(now, "Authentication time must not be null");
        return withAuthenticationState(0, null, now);
    }

    public LocalAuthenticationUser authenticationFailed(
            Instant now,
            int maximumFailedAttempts,
            Duration lockDuration
    ) {
        Preconditions.requireNonNull(now, "Authentication time must not be null");
        Preconditions.requireNonNull(lockDuration, "Lock duration must not be null");

        if (maximumFailedAttempts < 1) {
            throw new IllegalArgumentException("Maximum failed attempts must be positive");
        }
        if (lockDuration.isZero() || lockDuration.isNegative()) {
            throw new IllegalArgumentException("Lock duration must be positive");
        }

        int nextFailedAttempts = failedAttempts + 1;
        Instant nextLockedUntil =
                nextFailedAttempts >= maximumFailedAttempts
                        ? now.plus(lockDuration)
                        : null;

        return withAuthenticationState(
                nextFailedAttempts,
                nextLockedUntil,
                lastAuthenticatedAt
        );
    }

    private LocalAuthenticationUser withAuthenticationState(
            int nextFailedAttempts,
            Instant nextLockedUntil,
            Instant nextLastAuthenticatedAt
    ) {
        return new LocalAuthenticationUser(
                id,
                subject,
                username,
                passwordHash,
                status,
                authorities,
                nextFailedAttempts,
                nextLockedUntil,
                nextLastAuthenticatedAt
        );
    }
}
