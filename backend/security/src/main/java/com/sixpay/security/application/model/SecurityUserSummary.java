package com.sixpay.security.application.model;

import com.sixpay.security.domain.authentication.SixpayUserAccountStatus;

import java.time.Instant;
import java.util.UUID;

public record SecurityUserSummary(
        UUID id,
        String username,
        String email,
        SixpayUserAccountStatus status,
        boolean localEnabled,
        boolean oidcLinked,
        Instant lastAuthenticationAt
) {
}
