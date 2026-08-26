package com.sixpay.customer.observation.application.query;

import com.sixpay.customer.observation.domain.model.ObservedCustomerId;
import com.sixpay.customer.observation.domain.model.ObservedPaymentStatus;

import java.time.Instant;
import java.util.Objects;

/**
 * Summary view returned by Observed Customer searches.
 */
public record ObservedCustomerSummaryView(
        ObservedCustomerId observedCustomerId,
        MaskedIdentifierView niu,
        String legalName,
        MaskedIdentifierView phone,
        MaskedIdentifierView email,
        Instant firstObservedAt,
        Instant lastObservedAt,
        long totalPayments,
        long successfulPayments,
        long failedPayments,
        ObservedPaymentStatus lastPaymentStatus,
        String lastFailureReasonCode,
        Instant projectionUpdatedAt,
        long projectionVersion
) {

    public ObservedCustomerSummaryView {
        observedCustomerId = Objects.requireNonNull(
                observedCustomerId,
                "observedCustomerId is required"
        );

        niu = Objects.requireNonNull(
                niu,
                "niu is required"
        );

        legalName = requiredText(
                legalName,
                200,
                "legalName"
        );

        firstObservedAt = Objects.requireNonNull(
                firstObservedAt,
                "firstObservedAt is required"
        );

        lastObservedAt = Objects.requireNonNull(
                lastObservedAt,
                "lastObservedAt is required"
        );

        if (lastObservedAt.isBefore(firstObservedAt)) {
            throw new IllegalArgumentException(
                    "lastObservedAt must not precede firstObservedAt"
            );
        }

        requireCounters(
                totalPayments,
                successfulPayments,
                failedPayments
        );

        lastFailureReasonCode = optionalText(
                lastFailureReasonCode,
                64,
                "lastFailureReasonCode"
        );

        projectionUpdatedAt = Objects.requireNonNull(
                projectionUpdatedAt,
                "projectionUpdatedAt is required"
        );

        if (projectionVersion < 0) {
            throw new IllegalArgumentException(
                    "projectionVersion must not be negative"
            );
        }
    }

    private static void requireCounters(
            long total,
            long successful,
            long failed
    ) {
        if (total < 0 || successful < 0 || failed < 0) {
            throw new IllegalArgumentException(
                    "payment counters must not be negative"
            );
        }

        if (successful + failed > total) {
            throw new IllegalArgumentException(
                    "successful and failed counters must not exceed total"
            );
        }
    }

    private static String requiredText(
            String value,
            int maxLength,
            String field
    ) {
        String normalized = optionalText(
                value,
                maxLength,
                field
        );

        if (normalized == null) {
            throw new IllegalArgumentException(
                    field + " is required"
            );
        }

        return normalized;
    }

    private static String optionalText(
            String value,
            int maxLength,
            String field
    ) {
        if (value == null) {
            return null;
        }

        String normalized = value.strip();

        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(
                    field + " must not be blank"
            );
        }

        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(
                    field + " must not exceed "
                            + maxLength
                            + " characters"
            );
        }

        return normalized;
    }

    @Override
    public String toString() {
        return "ObservedCustomerSummaryView["
                + "observedCustomerId="
                + observedCustomerId
                + ", niu=[PROTECTED]"
                + ", legalName=[PROTECTED]"
                + ", phone="
                + (phone == null ? null : "[PROTECTED]")
                + ", email="
                + (email == null ? null : "[PROTECTED]")
                + ", firstObservedAt="
                + firstObservedAt
                + ", lastObservedAt="
                + lastObservedAt
                + ", totalPayments="
                + totalPayments
                + ", successfulPayments="
                + successfulPayments
                + ", failedPayments="
                + failedPayments
                + ", lastPaymentStatus="
                + lastPaymentStatus
                + ", lastFailureReasonCode="
                + lastFailureReasonCode
                + ", projectionUpdatedAt="
                + projectionUpdatedAt
                + ", projectionVersion="
                + projectionVersion
                + "]";
    }
}
