package com.sixpay.security.application.model;

import com.sixpay.security.domain.authentication.AuthenticationIdentityType;

import java.time.Instant;
import java.util.UUID;

public record SecurityIdentityView(
        UUID id,
        AuthenticationIdentityType identityType,
        String provider,
        String providerSubject,
        String status,
        Instant createdAt,
        Instant updatedAt
) {
}
