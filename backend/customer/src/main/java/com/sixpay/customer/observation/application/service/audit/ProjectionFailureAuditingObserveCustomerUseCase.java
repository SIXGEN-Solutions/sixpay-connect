package com.sixpay.customer.observation.application.service.audit;

import com.sixpay.customer.observation.application.audit
        .ObservedCustomerAuditAction;
import com.sixpay.customer.observation.application.audit
        .ObservedCustomerAuditContext;
import com.sixpay.customer.observation.application.audit
        .ObservedCustomerAuditOutcome;
import com.sixpay.customer.observation.application.audit
        .ObservedCustomerAuditRecord;
import com.sixpay.customer.observation.application.port.input
        .ObserveCustomerCommand;
import com.sixpay.customer.observation.application.port.input
        .ObserveCustomerResult;
import com.sixpay.customer.observation.application.port.input
        .ObserveCustomerUseCase;
import com.sixpay.customer.observation.application.port.output.audit
        .ObservedCustomerAuditIdGenerator;
import com.sixpay.customer.observation.application.port.output.audit
        .ObservedCustomerAuditPort;

import java.time.Clock;
import java.util.Objects;

/**
 * Records final projection failures after the transactional delegate has
 * rolled back.
 */
public final class ProjectionFailureAuditingObserveCustomerUseCase
        implements ObserveCustomerUseCase {

    private final ObserveCustomerUseCase delegate;
    private final ObservedCustomerAuditPort auditPort;
    private final ObservedCustomerAuditIdGenerator auditIdGenerator;
    private final Clock clock;

    public ProjectionFailureAuditingObserveCustomerUseCase(
            ObserveCustomerUseCase delegate,
            ObservedCustomerAuditPort auditPort,
            ObservedCustomerAuditIdGenerator auditIdGenerator,
            Clock clock
    ) {
        this.delegate = Objects.requireNonNull(delegate);
        this.auditPort = Objects.requireNonNull(auditPort);
        this.auditIdGenerator =
                Objects.requireNonNull(auditIdGenerator);
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    public ObserveCustomerResult observe(
            ObserveCustomerCommand command
    ) {
        Objects.requireNonNull(command, "command is required");

        try {
            return delegate.observe(command);
        } catch (RuntimeException projectionFailure) {
            try {
                auditPort.append(
                        ObservedCustomerAuditRecord.projection(
                                Objects.requireNonNull(
                                        auditIdGenerator.nextId(),
                                        "auditIdGenerator returned null"
                                ),
                                ObservedCustomerAuditAction
                                        .PROJECTION_FAILED,
                                ObservedCustomerAuditOutcome.FAILED,
                                null,
                                command.sourceEventId(),
                                command.paymentId(),
                                ObservedCustomerAuditContext.system(
                                        command.correlationId()
                                ),
                                clock.instant(),
                                "PROJECTION_FAILED"
                        )
                );
            } catch (RuntimeException auditFailure) {
                auditFailure.addSuppressed(projectionFailure);
                throw auditFailure;
            }

            throw projectionFailure;
        }
    }
}
