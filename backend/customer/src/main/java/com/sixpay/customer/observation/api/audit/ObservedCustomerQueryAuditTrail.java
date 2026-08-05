package com.sixpay.customer.observation.api.audit;

import com.sixpay.customer.observation.api.observability
        .ObservedCustomerQueryOperation;
import com.sixpay.customer.observation.api.observability
        .ObservedCustomerQueryResult;
import com.sixpay.customer.observation.application.audit
        .ObservedCustomerAuditAction;
import com.sixpay.customer.observation.application.audit
        .ObservedCustomerAuditContext;
import com.sixpay.customer.observation.application.audit
        .ObservedCustomerAuditOutcome;
import com.sixpay.customer.observation.application.audit
        .ObservedCustomerAuditRecord;
import com.sixpay.customer.observation.application.port.output.audit
        .ObservedCustomerAuditIdGenerator;
import com.sixpay.customer.observation.application.port.output.audit
        .ObservedCustomerAuditPort;
import com.sixpay.customer.observation.domain.model
        .ObservedCustomerId;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Clock;
import java.util.Objects;
import java.util.UUID;

/**
 * Fail-open audit writer for internal Observed Customer queries.
 */
public final class ObservedCustomerQueryAuditTrail {

    public static final String AUDIT_FAILURES =
            "sixpay.customer.observation.query.audit.failures";

    private static final Logger LOGGER =
            LoggerFactory.getLogger(
                    ObservedCustomerQueryAuditTrail.class
            );

    private final ObservedCustomerAuditPort auditPort;
    private final ObservedCustomerAuditIdGenerator auditIdGenerator;
    private final MeterRegistry meterRegistry;
    private final Clock clock;

    public ObservedCustomerQueryAuditTrail(
            ObservedCustomerAuditPort auditPort,
            ObservedCustomerAuditIdGenerator auditIdGenerator,
            MeterRegistry meterRegistry,
            Clock clock
    ) {
        this.auditPort = Objects.requireNonNull(auditPort);
        this.auditIdGenerator =
                Objects.requireNonNull(auditIdGenerator);
        this.meterRegistry = Objects.requireNonNull(meterRegistry);
        this.clock = Objects.requireNonNull(clock);
    }

    public void success(
            ObservedCustomerQueryOperation operation,
            UUID observedCustomerId,
            String correlationId
    ) {
        appendFailOpen(
                action(operation),
                ObservedCustomerAuditOutcome.SUCCEEDED,
                observedCustomerId,
                correlationId,
                null
        );
    }

    public void failure(
            ObservedCustomerQueryOperation operation,
            ObservedCustomerQueryResult result,
            UUID observedCustomerId,
            String correlationId
    ) {
        appendFailOpen(
                result == ObservedCustomerQueryResult.FORBIDDEN
                        || result
                        == ObservedCustomerQueryResult.UNAUTHORIZED
                        ? ObservedCustomerAuditAction.QUERY_DENIED
                        : ObservedCustomerAuditAction.QUERY_FAILED,
                outcome(result),
                observedCustomerId,
                correlationId,
                reasonCode(operation, result)
        );
    }

    public void denied(
            String correlationId
    ) {
        appendFailOpen(
                ObservedCustomerAuditAction.QUERY_DENIED,
                ObservedCustomerAuditOutcome.DENIED,
                null,
                correlationId,
                "ACCESS_DENIED"
        );
    }

    private void appendFailOpen(
            ObservedCustomerAuditAction action,
            ObservedCustomerAuditOutcome outcome,
            UUID observedCustomerId,
            String correlationId,
            String reasonCode
    ) {
        try {
            auditPort.append(
                    ObservedCustomerAuditRecord.query(
                            Objects.requireNonNull(
                                    auditIdGenerator.nextId(),
                                    "auditIdGenerator returned null"
                            ),
                            action,
                            outcome,
                            observedCustomerId == null
                                    ? null
                                    : ObservedCustomerId.of(
                                            observedCustomerId
                                    ),
                            new ObservedCustomerAuditContext(
                                    actorId(),
                                    safeCorrelationId(
                                            correlationId
                                    )
                            ),
                            clock.instant(),
                            reasonCode
                    )
            );
        } catch (RuntimeException auditFailure) {
            Counter.builder(AUDIT_FAILURES)
                    .tag("action", action.name())
                    .register(meterRegistry)
                    .increment();

            LOGGER.warn(
                    "Observed Customer query audit failed: "
                            + "action={}, outcome={}",
                    action,
                    outcome
            );
        }
    }

    private static ObservedCustomerAuditAction action(
            ObservedCustomerQueryOperation operation
    ) {
        return switch (operation) {
            case SEARCH ->
                    ObservedCustomerAuditAction.QUERY_SEARCHED;
            case GET ->
                    ObservedCustomerAuditAction.QUERY_DETAIL_READ;
            case LIST_PAYMENTS ->
                    ObservedCustomerAuditAction
                            .QUERY_PAYMENTS_LISTED;
        };
    }

    private static ObservedCustomerAuditOutcome outcome(
            ObservedCustomerQueryResult result
    ) {
        return switch (result) {
            case SUCCESS ->
                    ObservedCustomerAuditOutcome.SUCCEEDED;
            case FORBIDDEN, UNAUTHORIZED ->
                    ObservedCustomerAuditOutcome.DENIED;
            default ->
                    ObservedCustomerAuditOutcome.FAILED;
        };
    }

    private static String reasonCode(
            ObservedCustomerQueryOperation operation,
            ObservedCustomerQueryResult result
    ) {
        if (result == ObservedCustomerQueryResult.SUCCESS) {
            return null;
        }

        return operation.name()
                + "_"
                + switch (result) {
                    case NOT_FOUND -> "NOT_FOUND";
                    case INVALID -> "INVALID";
                    case UNAVAILABLE -> "UNAVAILABLE";
                    case FORBIDDEN, UNAUTHORIZED ->
                            "ACCESS_DENIED";
                    case RATE_LIMITED -> "RATE_LIMITED";
                    case INTERNAL_ERROR -> "INTERNAL_ERROR";
                    case SUCCESS -> "SUCCESS";
                };
    }

    private static String actorId() {
        Authentication authentication =
                SecurityContextHolder.getContext()
                        .getAuthentication();

        if (authentication == null
                || authentication.getName() == null
                || authentication.getName().isBlank()) {
            return "anonymous";
        }

        return authentication.getName();
    }

    private static String safeCorrelationId(
            String correlationId
    ) {
        return correlationId == null
                || correlationId.isBlank()
                ? "unavailable"
                : correlationId.strip();
    }
}
