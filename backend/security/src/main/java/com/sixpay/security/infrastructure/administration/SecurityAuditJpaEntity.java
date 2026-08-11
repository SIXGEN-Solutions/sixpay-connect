package com.sixpay.security.infrastructure.administration;

import com.sixpay.security.domain.administration.SecurityAuditEventType;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "security_audit_events")
public class SecurityAuditJpaEntity {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 64)
    private SecurityAuditEventType eventType;

    @Column(name = "actor_subject", length = 150)
    private String actorSubject;

    @Column(name = "target_user_id")
    private UUID targetUserId;

    @Column(length = 150)
    private String username;

    @Column(length = 500)
    private String provider;

    @Column(length = 500)
    private String detail;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected SecurityAuditJpaEntity() {
    }

    public SecurityAuditJpaEntity(
            UUID id,
            SecurityAuditEventType eventType,
            String actorSubject,
            UUID targetUserId,
            String username,
            String provider,
            String detail,
            Instant occurredAt
    ) {
        this.id = id;
        this.eventType = eventType;
        this.actorSubject = actorSubject;
        this.targetUserId = targetUserId;
        this.username = username;
        this.provider = provider;
        this.detail = detail;
        this.occurredAt = occurredAt;
    }

    public SecurityAuditEventType getEventType() { return eventType; }
    public String getActorSubject() { return actorSubject; }
    public String getProvider() { return provider; }
    public String getDetail() { return detail; }
    public Instant getOccurredAt() { return occurredAt; }
}
