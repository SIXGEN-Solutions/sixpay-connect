package com.sixpay.security.domain.authentication;

import com.sixpay.common.validation.Preconditions;

import java.time.Instant;
import java.util.UUID;

public record UserIdentity(
        UUID id,
        UUID userId,
        AuthenticationIdentityType identityType,
        String provider,
        String providerSubject,
        Instant createdAt,
        Instant updatedAt
) {
    public UserIdentity {
        id = Preconditions.requireNonNull(id, "User identity id must not be null");
        userId = Preconditions.requireNonNull(userId, "User identity user id must not be null");
        identityType = Preconditions.requireNonNull(identityType, "Identity type must not be null");
        provider = Preconditions.requireNonBlank(provider, "Identity provider must not be blank");
        providerSubject = Preconditions.requireNonBlank(providerSubject, "Provider subject must not be blank");
        createdAt = Preconditions.requireNonNull(createdAt, "Identity createdAt must not be null");
        updatedAt = Preconditions.requireNonNull(updatedAt, "Identity updatedAt must not be null");
    }
}
