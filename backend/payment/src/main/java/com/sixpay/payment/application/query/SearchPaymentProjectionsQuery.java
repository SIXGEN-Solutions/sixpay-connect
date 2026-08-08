package com.sixpay.payment.application.query;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record SearchPaymentProjectionsQuery(
        String cursor, int size, String paymentReference,
        String tresorPayRequestId, UUID observedCustomerId,
        String financialInstitutionCode, String status,
        String reasonCode, Instant createdFrom, Instant createdTo,
        BigDecimal amountMin, BigDecimal amountMax, String currency,
        PaymentSearchSort sort
) {
    public SearchPaymentProjectionsQuery {
        if (size < 1 || size > 200) {
            throw new IllegalArgumentException(
                    "Payment search size must be between 1 and 200"
            );
        }
        if (createdFrom != null
                && createdTo != null
                && createdFrom.isAfter(createdTo)) {
            throw new IllegalArgumentException(
                    "createdFrom must not be after createdTo"
            );
        }
        if (amountMin != null && amountMin.signum() < 0) {
            throw new IllegalArgumentException(
                    "amountMin must not be negative"
            );
        }
        if (amountMax != null && amountMax.signum() < 0) {
            throw new IllegalArgumentException(
                    "amountMax must not be negative"
            );
        }
        if (amountMin != null
                && amountMax != null
                && amountMin.compareTo(amountMax) > 0) {
            throw new IllegalArgumentException(
                    "amountMin must not exceed amountMax"
            );
        }
        sort = sort == null
                ? PaymentSearchSort.CREATED_AT_DESC
                : sort;
    }
}
