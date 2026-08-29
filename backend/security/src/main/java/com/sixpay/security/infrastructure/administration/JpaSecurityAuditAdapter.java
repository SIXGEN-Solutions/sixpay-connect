package com.sixpay.security.infrastructure.administration;

import com.sixpay.security.application.port.output.SecurityAuditPort;
import com.sixpay.security.domain.administration.SecurityAuditEvent;

import java.util.Objects;
import java.util.UUID;

public final class JpaSecurityAuditAdapter implements SecurityAuditPort {

    private final SecurityAuditSpringDataRepository repository;

    public JpaSecurityAuditAdapter(SecurityAuditSpringDataRepository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    @Override
    public void record(SecurityAuditEvent event) {
        repository.save(new SecurityAuditJpaEntity(
                UUID.randomUUID(),
                event.eventType(),
                sanitize(event.actorSubject()),
                event.targetUserId(),
                sanitize(event.username()),
                sanitize(event.provider()),
                sanitize(event.detail()),
                event.occurredAt()
        ));
    }

    private static String sanitize(String value) {
        if (value == null) {
            return null;
        }
        return value.replaceAll("[\\r\\n\\t]", "_");
    }
}
