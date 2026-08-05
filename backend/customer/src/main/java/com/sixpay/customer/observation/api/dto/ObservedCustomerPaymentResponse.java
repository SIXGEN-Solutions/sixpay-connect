package com.sixpay.customer.observation.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record ObservedCustomerPaymentResponse(
        UUID paymentId,
        String paymentReference,
        String financialInstitutionCode,
        AmountResponse amount,
        String status,
        String reasonCode,
        Instant createdAt,
        Instant updatedAt
) {

    public ObservedCustomerPaymentResponse {
        paymentId = Objects.requireNonNull(
                paymentId,
                "paymentId is required"
        );
        paymentReference = Objects.requireNonNull(
                paymentReference,
                "paymentReference is required"
        );
        financialInstitutionCode = Objects.requireNonNull(
                financialInstitutionCode,
                "financialInstitutionCode is required"
        );
        status = Objects.requireNonNull(
                status,
                "status is required"
        );
        createdAt = Objects.requireNonNull(
                createdAt,
                "createdAt is required"
        );
        updatedAt = Objects.requireNonNull(
                updatedAt,
                "updatedAt is required"
        );
    }

    public record AmountResponse(
            BigDecimal amount,
            String currency
    ) {

        public AmountResponse {
            amount = Objects.requireNonNull(
                    amount,
                    "amount is required"
            );
            currency = Objects.requireNonNull(
                    currency,
                    "currency is required"
            );

            if (amount.signum() < 0) {
                throw new IllegalArgumentException(
                        "amount must not be negative"
                );
            }

            if (!currency.matches("^[A-Z]{3}$")) {
                throw new IllegalArgumentException(
                        "currency must be an ISO 4217 code"
                );
            }
        }
    }
}
