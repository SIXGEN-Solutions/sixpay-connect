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
import com.sixpay.customer.observation.domain.model
        .ObservedPaymentStatus;

import java.time.Clock;
import java.util.Objects;

/**
 * Success-path projection audit decorator.
 *
 * <p>This decorator must execute inside the same transaction as the projection
 * writes. Any audit failure therefore aborts the complete mutation.</p>
 */
public final class AuditedObserveCustomerUseCase
        implements ObserveCustomerUseCase {

    private final ObserveCustomerUseCase delegate;
    private final ObservedCustomerAuditPort auditPort;
    private final ObservedCustomerAuditIdGenerator auditIdGenerator;
    private final Clock clock;

    public AuditedObserveCustomerUseCase(
            ObserveCustomerUseCase delegate,
            ObservedCustomerAuditPort auditPort,
            ObservedCustomerAuditIdGenerator auditIdGenerator,
            Clock clock
    ) {
        this.delegate = Objects.requireNonNull(
                delegate,
                "delegate is required"
        );
        this.auditPort = Objects.requireNonNull(
                auditPort,
                "auditPort is required"
        );
        this.auditIdGenerator = Objects.requireNonNull(
                auditIdGenerator,
                "auditIdGenerator is required"
        );
        this.clock = Objects.requireNonNull(
                clock,
                "clock is required"
        );
    }

    @Override
    public ObserveCustomerResult observe(
            ObserveCustomerCommand command
    ) {
        Objects.requireNonNull(command, "command is required");

        ObserveCustomerResult result =
                delegate.observe(command);

        auditPort.append(
                ObservedCustomerAuditRecord.projection(
                        Objects.requireNonNull(
                                auditIdGenerator.nextId(),
                                "auditIdGenerator returned null"
                        ),
                        action(command, result),
                        outcome(result),
                        result.observedCustomerId(),
                        command.sourceEventId(),
                        command.paymentId(),
                        ObservedCustomerAuditContext.system(
                                command.correlationId()
                        ),
                        clock.instant(),
                        reasonCode(command, result)
                )
        );

        return result;
    }

    private static ObservedCustomerAuditAction action(
            ObserveCustomerCommand command,
            ObserveCustomerResult result
    ) {
        if (command.paymentStatus()
                == ObservedPaymentStatus.REJECTED) {
            return ObservedCustomerAuditAction
                    .PROJECTION_REJECTED;
        }

        return switch (result.disposition()) {
            case APPLIED ->
                    ObservedCustomerAuditAction
                            .PROJECTION_APPLIED;
            case REPLAYED ->
                    ObservedCustomerAuditAction
                            .PROJECTION_REPLAYED;
            case IGNORED_STALE ->
                    ObservedCustomerAuditAction
                            .PROJECTION_STALE_IGNORED;
        };
    }

    private static ObservedCustomerAuditOutcome outcome(
            ObserveCustomerResult result
    ) {
        return switch (result.disposition()) {
            case APPLIED ->
                    ObservedCustomerAuditOutcome.SUCCEEDED;
            case REPLAYED ->
                    ObservedCustomerAuditOutcome.REPLAYED;
            case IGNORED_STALE ->
                    ObservedCustomerAuditOutcome.IGNORED;
        };
    }

    private static String reasonCode(
            ObserveCustomerCommand command,
            ObserveCustomerResult result
    ) {
        if (command.paymentStatus()
                == ObservedPaymentStatus.REJECTED) {
            return command.failureReasonCode() == null
                    ? "PAYMENT_REJECTED"
                    : command.failureReasonCode();
        }

        if (result.disposition()
                == ObserveCustomerResult.Disposition.IGNORED_STALE) {
            return "STALE_EVENT";
        }

        return null;
    }
}
