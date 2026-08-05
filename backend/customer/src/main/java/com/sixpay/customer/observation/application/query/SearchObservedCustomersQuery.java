package com.sixpay.customer.observation.application.query;

import com.sixpay.customer.observation.domain.model.ObservedPaymentStatus;

import java.time.Instant;
import java.util.Objects;

/**
 * Customer-owned criteria for stable Observed Customer search.
 *
 * <p>{@code snapshotAt} is established for the first page and must be reused
 * unchanged for all subsequent pages represented by a cursor.</p>
 */
public record SearchObservedCustomersQuery(
        String normalizedNiu,
        String legalName,
        String financialInstitutionCode,
        ObservedPaymentStatus lastPaymentStatus,
        String lastFailureReasonCode,
        Instant firstObservedFrom,
        Instant firstObservedTo,
        Instant lastObservedFrom,
        Instant lastObservedTo,
        Instant paymentFrom,
        Instant paymentTo,
        ObservedCustomerSort sort,
        ObservedCustomerCursor cursor,
        int size,
        Instant snapshotAt
) {

    public static final int DEFAULT_SIZE = 50;
    public static final int MAX_SIZE = 200;

    public SearchObservedCustomersQuery {
        normalizedNiu = optionalText(
                normalizedNiu,
                64,
                "normalizedNiu"
        );
        legalName = optionalText(
                legalName,
                200,
                "legalName"
        );
        financialInstitutionCode = optionalText(
                financialInstitutionCode,
                32,
                "financialInstitutionCode"
        );
        lastFailureReasonCode = optionalText(
                lastFailureReasonCode,
                64,
                "lastFailureReasonCode"
        );

        requireOrdered(
                firstObservedFrom,
                firstObservedTo,
                "firstObserved"
        );
        requireOrdered(
                lastObservedFrom,
                lastObservedTo,
                "lastObserved"
        );
        requireOrdered(
                paymentFrom,
                paymentTo,
                "payment"
        );

        sort = sort == null
                ? ObservedCustomerSort.LAST_OBSERVED_AT_DESC
                : sort;

        if (size < 1 || size > MAX_SIZE) {
            throw new IllegalArgumentException(
                    "size must be between 1 and " + MAX_SIZE
            );
        }

        snapshotAt = Objects.requireNonNull(
                snapshotAt,
                "snapshotAt is required"
        );
    }

    public static SearchObservedCustomersQuery firstPage(
            String normalizedNiu,
            String legalName,
            String financialInstitutionCode,
            ObservedPaymentStatus lastPaymentStatus,
            String lastFailureReasonCode,
            Instant firstObservedFrom,
            Instant firstObservedTo,
            Instant lastObservedFrom,
            Instant lastObservedTo,
            Instant paymentFrom,
            Instant paymentTo,
            ObservedCustomerSort sort,
            Integer size,
            Instant snapshotAt
    ) {
        return new SearchObservedCustomersQuery(
                normalizedNiu,
                legalName,
                financialInstitutionCode,
                lastPaymentStatus,
                lastFailureReasonCode,
                firstObservedFrom,
                firstObservedTo,
                lastObservedFrom,
                lastObservedTo,
                paymentFrom,
                paymentTo,
                sort,
                null,
                size == null ? DEFAULT_SIZE : size,
                snapshotAt
        );
    }

    public boolean continuationPage() {
        return cursor != null;
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

    private static void requireOrdered(
            Instant from,
            Instant to,
            String field
    ) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException(
                    field + " from must not be after to"
            );
        }
    }

    @Override
    public String toString() {
        return "SearchObservedCustomersQuery["
                + "normalizedNiu=[PROTECTED]"
                + ", legalName=[PROTECTED]"
                + ", financialInstitutionCode="
                + financialInstitutionCode
                + ", lastPaymentStatus="
                + lastPaymentStatus
                + ", lastFailureReasonCode="
                + lastFailureReasonCode
                + ", firstObservedFrom="
                + firstObservedFrom
                + ", firstObservedTo="
                + firstObservedTo
                + ", lastObservedFrom="
                + lastObservedFrom
                + ", lastObservedTo="
                + lastObservedTo
                + ", paymentFrom="
                + paymentFrom
                + ", paymentTo="
                + paymentTo
                + ", sort="
                + sort
                + ", cursor="
                + (cursor == null ? null : "[PROTECTED]")
                + ", size="
                + size
                + ", snapshotAt="
                + snapshotAt
                + "]";
    }
}
