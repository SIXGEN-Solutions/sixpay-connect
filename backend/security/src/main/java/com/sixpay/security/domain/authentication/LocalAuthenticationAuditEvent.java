package com.sixpay.security.domain.authentication;

import com.sixpay.common.validation.Preconditions;

import java.time.Instant;

public record LocalAuthenticationAuditEvent(
        LocalAuthenticationAuditType type,
        String subject,
        String username,
        LocalAuthenticationAuditOutcome outcome,
        Instant occurredAt
) {
    public LocalAuthenticationAuditEvent {
        type = Preconditions.requireNonNull(type, "Authentication audit type must not be null");
        username = Preconditions.requireNonBlank(username, "Authentication audit username must not be blank");
        outcome = Preconditions.requireNonNull(outcome, "Authentication audit outcome must not be null");
        occurredAt = Preconditions.requireNonNull(occurredAt, "Authentication audit time must not be null");
    }
}
