package com.sixpay.security.domain.authentication;

import com.sixpay.common.validation.Preconditions;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** SIXPAY-owned lifecycle state of a LOCAL credential. */
public record LocalCredential(
        UUID userId,
        String passwordHash,
        boolean mustChangePassword,
        Instant passwordChangedAt,
        Instant expiresAt,
        Instant updatedAt
) {
    public LocalCredential {
        userId = Preconditions.requireNonNull(userId, "Credential SIXPAY user id must not be null");
        passwordHash = Preconditions.requireNonBlank(passwordHash, "Credential password hash must not be blank");
        updatedAt = Preconditions.requireNonNull(updatedAt, "Credential update time must not be null");
        if (expiresAt != null && passwordChangedAt == null) {
            throw new IllegalArgumentException("Password expiration requires a password change timestamp");
        }
        if (passwordChangedAt != null && expiresAt != null && !expiresAt.isAfter(passwordChangedAt)) {
            throw new IllegalArgumentException("Password expiration must be after password change time");
        }
        if (mustChangePassword && expiresAt != null) {
            throw new IllegalArgumentException("Temporary password must not have a normal expiration timestamp");
        }
    }

    public static LocalCredential provisioned(UUID userId, String passwordHash, Instant now) {
        return temporary(userId, passwordHash, now);
    }

    public LocalCredential reset(String temporaryPasswordHash, Instant now) {
        return temporary(userId, temporaryPasswordHash, Objects.requireNonNull(now, "Password reset time must not be null"));
    }

    public LocalCredential changedByUser(String newPasswordHash, Instant now, PasswordPolicy policy) {
        Preconditions.requireNonBlank(newPasswordHash, "New password hash must not be blank");
        Objects.requireNonNull(now, "Password change time must not be null");
        Objects.requireNonNull(policy, "Password policy must not be null");
        return new LocalCredential(
                userId,
                newPasswordHash,
                false,
                now,
                now.plus(Duration.ofDays(policy.expirationDays())),
                now
        );
    }

    public boolean expiredAt(Instant now) {
        Objects.requireNonNull(now, "Current time must not be null");
        return expiresAt != null && !now.isBefore(expiresAt);
    }

    public boolean changeRequiredAt(Instant now) {
        return mustChangePassword || expiredAt(now);
    }

    private static LocalCredential temporary(UUID userId, String passwordHash, Instant now) {
        return new LocalCredential(
                userId,
                passwordHash,
                true,
                null,
                null,
                Objects.requireNonNull(now, "Credential update time must not be null")
        );
    }
}
