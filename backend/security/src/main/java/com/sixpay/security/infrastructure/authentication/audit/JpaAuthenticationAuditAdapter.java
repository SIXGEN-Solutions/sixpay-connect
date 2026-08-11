package com.sixpay.security.infrastructure.authentication.audit;

import com.sixpay.security.application.port.out.AuthenticationAuditPort;
import com.sixpay.security.domain.authentication.LocalAuthenticationAuditEvent;

import java.util.Objects;
import java.util.UUID;

public final class JpaAuthenticationAuditAdapter
        implements AuthenticationAuditPort {

    private final AuthenticationAuditSpringDataRepository repository;

    public JpaAuthenticationAuditAdapter(
            AuthenticationAuditSpringDataRepository repository
    ) {
        this.repository = Objects.requireNonNull(repository);
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
    }
}
