package com.sixpay.security.infrastructure.authentication.audit;

import com.sixpay.security.application.port.out.AuthenticationAuditPort;
import com.sixpay.security.application.port.out.SecurityAuditPort;
import com.sixpay.security.domain.administration.SecurityAuditEvent;
import com.sixpay.security.domain.administration.SecurityAuditEventType;
import com.sixpay.security.domain.authentication.LocalAuthenticationAuditEvent;
import com.sixpay.security.domain.authentication.LocalAuthenticationAuditOutcome;
import com.sixpay.security.domain.authentication.LocalAuthenticationAuditType;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class JpaAuthenticationAuditAdapter
        implements AuthenticationAuditPort {

    private final AuthenticationAuditSpringDataRepository repository;
    private final SecurityAuditPort securityAuditPort;

    public JpaAuthenticationAuditAdapter(
            AuthenticationAuditSpringDataRepository repository,
            SecurityAuditPort securityAuditPort
    ) {
        this.repository = Objects.requireNonNull(repository);
        this.securityAuditPort = Objects.requireNonNull(securityAuditPort);
    }

    @Override
    public void record(LocalAuthenticationAuditEvent event) {
        repository.save(
                new AuthenticationAuditJpaEntity(
                        UUID.randomUUID(),
                        event.type(),
                        event.subject(),
                        event.username(),
                        event.outcome(),
                        event.occurredAt()
                )
        );

        SecurityAuditEventType type =
                event.type() == LocalAuthenticationAuditType.LOGOUT
                        ? SecurityAuditEventType.LOGOUT
                        : event.outcome() == LocalAuthenticationAuditOutcome.SUCCESS
                        ? SecurityAuditEventType.LOGIN_SUCCESS
                        : SecurityAuditEventType.LOGIN_FAILURE;

        securityAuditPort.record(new SecurityAuditEvent(
                type,
                event.subject(),
                parseUserId(event.subject()),
                event.username(),
                "SIXPAY",
                null,
                event.occurredAt()
        ));
    }

    @Override
    public void recordAccountLocked(
            String subject,
            String username,
            Instant occurredAt
    ) {
        securityAuditPort.record(new SecurityAuditEvent(
                SecurityAuditEventType.ACCOUNT_LOCKED,
                subject,
                parseUserId(subject),
                username,
                "SIXPAY",
                null,
                occurredAt
        ));
    }

    private static UUID parseUserId(String subject) {
        if (subject == null) {
            return null;
        }
        try {
            return UUID.fromString(subject);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
