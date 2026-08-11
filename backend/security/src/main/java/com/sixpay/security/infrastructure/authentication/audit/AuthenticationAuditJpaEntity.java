package com.sixpay.security.infrastructure.authentication.audit;

import com.sixpay.security.domain.authentication.LocalAuthenticationAuditOutcome;
import com.sixpay.security.domain.authentication.LocalAuthenticationAuditType;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "security_authentication_audit")
public class AuthenticationAuditJpaEntity {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 32)
    private LocalAuthenticationAuditType eventType;

    @Column(length = 150)
    private String subject;

    @Column(nullable = false, length = 150)
    private String username;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private LocalAuthenticationAuditOutcome outcome;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected AuthenticationAuditJpaEntity() {
    }

    AuthenticationAuditJpaEntity(
            UUID id,
            LocalAuthenticationAuditType eventType,
            String subject,
            String username,
            LocalAuthenticationAuditOutcome outcome,
            Instant occurredAt
    ) {
        this.id = id;
        this.eventType = eventType;
        this.subject = subject;
        this.username = username;
        this.outcome = outcome;
        this.occurredAt = occurredAt;
    }
}
