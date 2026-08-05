package com.sixpay.customer.observation.application.audit;

import com.sixpay.customer.observation.domain.model.ObservedCustomerId;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record ObservedCustomerAuditRecord(
        UUID auditId,
        ObservedCustomerAuditAction action,
        ObservedCustomerAuditOutcome outcome,
        ObservedCustomerId observedCustomerId,
        UUID sourceEventId,
        UUID paymentId,
        String actorId,
        String correlationId,
        Instant occurredAt,
        String reasonCode
) {
    public static final int MAX_REASON_CODE_LENGTH = 100;

    public ObservedCustomerAuditRecord {
        auditId = Objects.requireNonNull(auditId, "auditId is required");
        action = Objects.requireNonNull(action, "action is required");
        outcome = Objects.requireNonNull(outcome, "outcome is required");

        ObservedCustomerAuditContext context =
                new ObservedCustomerAuditContext(
                        actorId,
                        correlationId
                );
        actorId = context.actorId();
        correlationId = context.correlationId();

        occurredAt = Objects.requireNonNull(
                occurredAt,
                "occurredAt is required"
        );
        reasonCode = normalizeReasonCode(reasonCode);

        validateReferences(
                action,
                observedCustomerId,
                sourceEventId,
                paymentId
        );
    }

    public static ObservedCustomerAuditRecord projection(
            UUID auditId,
            ObservedCustomerAuditAction action,
            ObservedCustomerAuditOutcome outcome,
            ObservedCustomerId observedCustomerId,
            UUID sourceEventId,
            UUID paymentId,
            ObservedCustomerAuditContext context,
            Instant occurredAt,
            String reasonCode
    ) {
        Objects.requireNonNull(context, "context is required");
        if (!isProjectionAction(action)) {
            throw new IllegalArgumentException(
                    "projection factory requires a projection action"
            );
        }
        return new ObservedCustomerAuditRecord(
                auditId,
                action,
                outcome,
                observedCustomerId,
                sourceEventId,
                paymentId,
                context.actorId(),
                context.correlationId(),
                occurredAt,
                reasonCode
        );
    }

    public static ObservedCustomerAuditRecord query(
            UUID auditId,
            ObservedCustomerAuditAction action,
            ObservedCustomerAuditOutcome outcome,
            ObservedCustomerId observedCustomerId,
            ObservedCustomerAuditContext context,
            Instant occurredAt,
            String reasonCode
    ) {
        Objects.requireNonNull(context, "context is required");
        if (isProjectionAction(action)) {
            throw new IllegalArgumentException(
                    "query factory requires a query action"
            );
        }
        return new ObservedCustomerAuditRecord(
                auditId,
                action,
                outcome,
                observedCustomerId,
                null,
                null,
                context.actorId(),
                context.correlationId(),
                occurredAt,
                reasonCode
        );
    }

    public ObservedCustomerAuditContext context() {
        return new ObservedCustomerAuditContext(
                actorId,
                correlationId
        );
    }

    private static void validateReferences(
            ObservedCustomerAuditAction action,
            ObservedCustomerId observedCustomerId,
            UUID sourceEventId,
            UUID paymentId
    ) {
        if (isProjectionAction(action)) {
            Objects.requireNonNull(
                    sourceEventId,
                    "sourceEventId is required for projection audit"
            );
            Objects.requireNonNull(
                    paymentId,
                    "paymentId is required for projection audit"
            );
        }

        if (action == ObservedCustomerAuditAction.QUERY_DETAIL_READ
                || action == ObservedCustomerAuditAction
                .QUERY_PAYMENTS_LISTED) {
            Objects.requireNonNull(
                    observedCustomerId,
                    "observedCustomerId is required for this query audit"
            );
        }
    }

    private static boolean isProjectionAction(
            ObservedCustomerAuditAction action
    ) {
        return switch (action) {
            case PROJECTION_APPLIED,
                 PROJECTION_REPLAYED,
                 PROJECTION_STALE_IGNORED,
                 PROJECTION_REJECTED -> true;
            default -> false;
        };
    }

    private static String normalizeReasonCode(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.strip();
        if (normalized.length() > MAX_REASON_CODE_LENGTH) {
            throw new IllegalArgumentException(
                    "reasonCode must not exceed "
                            + MAX_REASON_CODE_LENGTH
                            + " characters"
            );
        }
        if (!normalized.matches("^[A-Z0-9][A-Z0-9_.-]*$")) {
            throw new IllegalArgumentException(
                    "reasonCode must be a technical code"
            );
        }
        return normalized;
    }

    @Override
    public String toString() {
        return "ObservedCustomerAuditRecord["
                + "auditId=" + auditId
                + ", action=" + action
                + ", outcome=" + outcome
                + ", observedCustomerId=" + observedCustomerId
                + ", sourceEventId=" + sourceEventId
                + ", paymentId=" + paymentId
                + ", actorId=[PROTECTED]"
                + ", correlationId=" + correlationId
                + ", occurredAt=" + occurredAt
                + ", reasonCode=" + reasonCode
                + "]";
    }
}
