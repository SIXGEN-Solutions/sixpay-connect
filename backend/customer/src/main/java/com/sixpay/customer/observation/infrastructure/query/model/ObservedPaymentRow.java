package com.sixpay.customer.observation.infrastructure.query.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Flat SQL row for one linked observed Payment.
 */
public record ObservedPaymentRow(
        UUID paymentId,
        String publicPaymentReference,
        String financialInstitutionCode,
        BigDecimal amount,
        String currency,
        String paymentStatus,
        String failureReasonCode,
        Instant paymentCreatedAt,
        Instant paymentUpdatedAt
) {

    public ObservedPaymentRow {
        paymentId = Objects.requireNonNull(
                paymentId,
                "paymentId is required"
        );
        publicPaymentReference = Objects.requireNonNull(
                publicPaymentReference,
                "publicPaymentReference is required"
        );
        financialInstitutionCode = Objects.requireNonNull(
                financialInstitutionCode,
                "financialInstitutionCode is required"
        );
        amount = Objects.requireNonNull(
                amount,
                "amount is required"
        );
        currency = Objects.requireNonNull(
                currency,
                "currency is required"
        );
        paymentStatus = Objects.requireNonNull(
                paymentStatus,
                "paymentStatus is required"
        );
        paymentCreatedAt = Objects.requireNonNull(
                paymentCreatedAt,
                "paymentCreatedAt is required"
        );
        paymentUpdatedAt = Objects.requireNonNull(
                paymentUpdatedAt,
                "paymentUpdatedAt is required"
        );
    }
}
