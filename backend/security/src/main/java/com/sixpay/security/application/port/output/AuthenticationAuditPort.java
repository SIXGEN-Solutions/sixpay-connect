package com.sixpay.security.application.port.output;

import com.sixpay.security.domain.authentication.LocalAuthenticationAuditEvent;

import java.time.Instant;

public interface AuthenticationAuditPort {

    void record(LocalAuthenticationAuditEvent event);

    default void recordAccountLocked(
            String subject,
            String username,
            Instant occurredAt
    ) {
        // Optional for legacy/test adapters. Production DA-9 adapter overrides.
    }
}
