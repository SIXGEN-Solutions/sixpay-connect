package com.sixpay.payment.application.event.projection;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Currency;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Version-one payload representing Payment state at source-event creation
 * time.
 */
public record ObservedCustomerProjectionPayload(
        String paymentReference,
        String normalizedNiu,
        String legalName,
        String phoneMasked,
        String emailMasked,
        String financialInstitutionCode,
        String accountBindingFingerprint,
        String maskedAccountReference,
        BigDecimal amount,
        String currency,
        ProjectionPaymentStatus paymentStatus,
        String failureReasonCode,
        Instant paymentCreatedAt,
        Instant paymentUpdatedAt
) {

    /**
     * Canonical monetary scale used by the version-one event contract.
     *
     * <p>The scale is part of the serialized schema. It guarantees stable
     * record equality after a JSON round trip.</p>
     */
    private static final int AMOUNT_SCALE = 2;

    private static final Pattern FINGERPRINT_PATTERN =
            Pattern.compile(
                    "^v1:[0-9a-f]{64}$"
            );

    private static final Pattern RAW_ACCOUNT_PATTERN =
            Pattern.compile(
                    ".*\\d{8,}.*"
            );

    public ObservedCustomerProjectionPayload {
        paymentReference = requireText(
                paymentReference,
                "paymentReference"
        );

        normalizedNiu = requireText(
                normalizedNiu,
                "normalizedNiu"
        );

        legalName = requireText(
                legalName,
                "legalName"
        );

        phoneMasked = normalizeOptional(
                phoneMasked
        );

        emailMasked = normalizeOptional(
                emailMasked
        );

        financialInstitutionCode = requireText(
                financialInstitutionCode,
                "financialInstitutionCode"
        ).toUpperCase(
                Locale.ROOT
        );

        accountBindingFingerprint =
                requireFingerprint(
                        accountBindingFingerprint
                );

        maskedAccountReference =
                requireMaskedAccountReference(
                        maskedAccountReference
                );

        amount = requireCanonicalAmount(
                amount
        );

        currency = requireCurrency(
                currency
        );

        paymentStatus = Objects.requireNonNull(
                paymentStatus,
                "paymentStatus is required"
        );

        failureReasonCode = normalizeOptional(
                failureReasonCode
        );

        paymentCreatedAt = Objects.requireNonNull(
                paymentCreatedAt,
                "paymentCreatedAt is required"
        );

        paymentUpdatedAt = Objects.requireNonNull(
                paymentUpdatedAt,
                "paymentUpdatedAt is required"
        );

        if (paymentUpdatedAt.isBefore(paymentCreatedAt)) {
            throw new IllegalArgumentException(
                    "paymentUpdatedAt must not precede "
                            + "paymentCreatedAt"
            );
        }

        validateFailureSemantics(
                paymentStatus,
                failureReasonCode
        );
    }

    private static BigDecimal requireCanonicalAmount(
            BigDecimal value
    ) {
        Objects.requireNonNull(
                value,
                "amount is required"
        );

        if (value.signum() < 0) {
            throw new IllegalArgumentException(
                    "amount must not be negative"
            );
        }

        try {
            return value.setScale(
                    AMOUNT_SCALE,
                    RoundingMode.UNNECESSARY
            );
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException(
                    "amount must have at most "
                            + AMOUNT_SCALE
                            + " fractional digits",
                    exception
            );
        }
    }

    private static void validateFailureSemantics(
            ProjectionPaymentStatus status,
            String failureReasonCode
    ) {
        boolean failureStatus =
                status == ProjectionPaymentStatus.REJECTED
                        || status
                        == ProjectionPaymentStatus.FAILED;

        if (failureStatus && failureReasonCode == null) {
            throw new IllegalArgumentException(
                    "failureReasonCode is required for "
                            + status
            );
        }

        if (!failureStatus && failureReasonCode != null) {
            throw new IllegalArgumentException(
                    "failureReasonCode is only allowed for "
                            + "REJECTED or FAILED"
            );
        }
    }

    private static String requireFingerprint(
            String value
    ) {
        String normalized = requireText(
                value,
                "accountBindingFingerprint"
        );

        if (!FINGERPRINT_PATTERN
                .matcher(normalized)
                .matches()) {

            throw new IllegalArgumentException(
                    "accountBindingFingerprint must match "
                            + "v1:<64 lowercase hexadecimal characters>"
            );
        }

        return normalized;
    }

    private static String requireMaskedAccountReference(
            String value
    ) {
        String normalized = requireText(
                value,
                "maskedAccountReference"
        );

        if (RAW_ACCOUNT_PATTERN
                .matcher(normalized)
                .matches()) {

            throw new IllegalArgumentException(
                    "maskedAccountReference appears to contain "
                            + "a raw account value"
            );
        }

        return normalized;
    }

    private static String requireCurrency(
            String value
    ) {
        String normalized = requireText(
                value,
                "currency"
        ).toUpperCase(
                Locale.ROOT
        );

        if (normalized.length() != 3) {
            throw new IllegalArgumentException(
                    "currency must contain three characters"
            );
        }

        try {
            Currency.getInstance(
                    normalized
            );
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "currency must be an ISO-4217 code",
                    exception
            );
        }

        return normalized;
    }

    private static String requireText(
            String value,
            String field
    ) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    field + " must not be blank"
            );
        }

        return value.strip();
    }

    private static String normalizeOptional(
            String value
    ) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.strip();
    }

    @Override
    public String toString() {
        return "ObservedCustomerProjectionPayload["
                + "paymentReference="
                + paymentReference
                + ", normalizedNiu=[PROTECTED]"
                + ", legalName=[PROTECTED]"
                + ", phoneMasked=[PROTECTED]"
                + ", emailMasked=[PROTECTED]"
                + ", financialInstitutionCode="
                + financialInstitutionCode
                + ", accountBindingFingerprint=[PROTECTED]"
                + ", maskedAccountReference=[PROTECTED]"
                + ", amount="
                + amount
                + ", currency="
                + currency
                + ", paymentStatus="
                + paymentStatus
                + ", failureReasonCode="
                + failureReasonCode
                + ", paymentCreatedAt="
                + paymentCreatedAt
                + ", paymentUpdatedAt="
                + paymentUpdatedAt
                + "]";
    }
}