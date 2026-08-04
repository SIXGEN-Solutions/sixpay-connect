package com.sixpay.customer.observation.domain.model;

import com.sixpay.customer.observation.domain.exception.ObservedCustomerDomainException;
import com.sixpay.sharedkernel.domain.valueobject.ValueObject;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

public record ObservedPaymentReference(
        UUID paymentId,
        String paymentReference,
        String financialInstitutionCode,
        BigDecimal amount,
        String currency,
        ObservedPaymentStatus status,
        String failureReasonCode,
        Instant createdAt,
        Instant updatedAt
) implements ValueObject {

    private static final UUID NIL_UUID = new UUID(0L, 0L);
    private static final Pattern CURRENCY = Pattern.compile("^[A-Z]{3}$");
    private static final Pattern REASON_CODE = Pattern.compile(
            "^[A-Z0-9][A-Z0-9._-]{0,63}$"
    );

    public ObservedPaymentReference {
        paymentId = Objects.requireNonNull(paymentId, "paymentId is required");
        if (NIL_UUID.equals(paymentId)) {
            throw new ObservedCustomerDomainException(
                    "paymentId must not be nil"
            );
        }

        paymentReference = requireText(
                paymentReference,
                "paymentReference",
                128
        );
        financialInstitutionCode = requireText(
                financialInstitutionCode,
                "financialInstitutionCode",
                32
        ).toUpperCase(Locale.ROOT);

        amount = Objects.requireNonNull(amount, "amount is required");
        if (amount.signum() < 0) {
            throw new ObservedCustomerDomainException(
                    "amount must not be negative"
            );
        }

        if (currency == null) {
            throw new ObservedCustomerDomainException(
                    "currency is required"
            );
        }
        currency = currency.strip().toUpperCase(Locale.ROOT);
        if (!CURRENCY.matcher(currency).matches()) {
            throw new ObservedCustomerDomainException(
                    "currency must be a three-letter uppercase code"
            );
        }

        status = Objects.requireNonNull(status, "status is required");

        if (failureReasonCode != null) {
            failureReasonCode = failureReasonCode
                    .strip()
                    .toUpperCase(Locale.ROOT);
            if (failureReasonCode.isEmpty()) {
                failureReasonCode = null;
            } else if (!REASON_CODE.matcher(failureReasonCode).matches()) {
                throw new ObservedCustomerDomainException(
                        "failureReasonCode has an invalid format"
                );
            }
        }

        createdAt = Objects.requireNonNull(createdAt, "createdAt is required");
        updatedAt = Objects.requireNonNull(updatedAt, "updatedAt is required");
        if (updatedAt.isBefore(createdAt)) {
            throw new ObservedCustomerDomainException(
                    "updatedAt must not be before createdAt"
            );
        }
    }

    public Optional<String> failureReasonCodeOptional() {
        return Optional.ofNullable(failureReasonCode);
    }

    private static String requireText(
            String value,
            String field,
            int maxLength
    ) {
        if (value == null) {
            throw new ObservedCustomerDomainException(
                    field + " is required"
            );
        }
        String normalized = value.strip();
        if (normalized.isEmpty()
                || normalized.length() > maxLength
                || normalized.chars().anyMatch(Character::isISOControl)) {
            throw new ObservedCustomerDomainException(
                    field + " has an invalid value"
            );
        }
        return normalized;
    }
}
