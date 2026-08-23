package com.sixpay.customer.management.application.audit;

import com.sixpay.common.context.CorrelationId;
import com.sixpay.customer.management.application.port.output.CustomerAuditTrail;
import com.sixpay.security.authentication.CurrentUserProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class CustomerAuditRecorder {

    private final CustomerAuditTrail auditTrail;
    private final CurrentUserProvider currentUserProvider;

    public CustomerAuditRecorder(
            CustomerAuditTrail auditTrail,
            CurrentUserProvider currentUserProvider
    ) {
        this.auditTrail = auditTrail;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional
    public void success(
            String aggregateType,
            UUID aggregateId,
            String action,
            String correlationId,
            String details
    ) {
        String actor = currentUserProvider
                .requireCurrentUser()
                .subject();

        String effectiveCorrelationId =
                correlationId == null || correlationId.isBlank()
                        ? CorrelationId.generate().value()
                        : CorrelationId.of(
                                correlationId.strip()
                        ).value();

        auditTrail.append(
                new CustomerAuditRecord(
                        UUID.randomUUID(),
                        aggregateType,
                        aggregateId,
                        action,
                        "SUCCESS",
                        actor,
                        effectiveCorrelationId,
                        details,
                        Instant.now()
                )
        );
    }
}
