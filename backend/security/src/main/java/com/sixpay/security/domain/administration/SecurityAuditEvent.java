package com.sixpay.security.domain.administration;

import java.time.Instant;
import java.util.UUID;

public record SecurityAuditEvent(
        SecurityAuditEventType eventType,
        String actorSubject,
        UUID targetUserId,
        String username,
        String provider,
        String detail,
        Instant occurredAt
) {
}
