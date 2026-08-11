package com.sixpay.security.application.model;

import com.sixpay.security.domain.administration.SecurityAuditEventType;

import java.time.Instant;

public record SecurityAuditView(
        SecurityAuditEventType eventType,
        String actorSubject,
        String provider,
        String detail,
        Instant occurredAt
) {
}
