package com.sixpay.customer.observation.application.query;

import com.sixpay.customer.observation.domain.model.ObservedCustomerId;
import com.sixpay.customer.observation.domain.model.ObservedPaymentStatus;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Detailed read-only Observed Customer projection.
 */
public record ObservedCustomerDetailView(
        ObservedCustomerId observedCustomerId,
        MaskedIdentifierView niu,
        String legalName,
        MaskedIdentifierView phone,
        MaskedIdentifierView email,
        List<ObservedInstitutionView> institutions,
        Instant firstObservedAt,
        Instant lastObservedAt,
        long totalPayments,
        long successfulPayments,
        long failedPayments,
        ObservedPaymentStatus lastPaymentStatus,
        String lastFailureReasonCode,
        Instant projectionUpdatedAt,
        long projectionVersion,
        String sourceEventWatermark
) {

    public ObservedCustomerDetailView {
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

        institutions = List.copyOf(
                Objects.requireNonNull(
                        institutions,
                        "institutions is required"
                )
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

        if (totalPayments < 0
                || successfulPayments < 0
                || failedPayments < 0) {
            throw new IllegalArgumentException(
                    "payment counters must not be negative"
            );
        }

        if (successfulPayments + failedPayments
                > totalPayments) {
            throw new IllegalArgumentException(
                    "successful and failed counters must not exceed total"
            );
        }

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

        sourceEventWatermark = requiredText(
                sourceEventWatermark,
                512,
                "sourceEventWatermark"
        );
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
        return "ObservedCustomerDetailView["
                + "observedCustomerId="
                + observedCustomerId
                + ", niu=[PROTECTED]"
                + ", legalName=[PROTECTED]"
                + ", phone="
                + (phone == null ? null : "[PROTECTED]")
                + ", email="
                + (email == null ? null : "[PROTECTED]")
                + ", institutions="
                + institutions.size()
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
                + ", sourceEventWatermark=[PROTECTED]"
                + "]";
    }
}
