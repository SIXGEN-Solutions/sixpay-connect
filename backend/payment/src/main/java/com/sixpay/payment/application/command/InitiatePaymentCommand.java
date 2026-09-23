package com.sixpay.payment.application.command;

import com.sixpay.common.context.CorrelationId;
import com.sixpay.payment.domain.model.ClaimType;
import com.sixpay.payment.domain.model.ExternalSubscriptionReference;
import com.sixpay.payment.domain.model.PaymentSource;
import com.sixpay.payment.domain.model.CanonicalPartnerIdentity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Provider-neutral application command for Payment initiation.
 *
 * <p>Authentication secrets, API keys, PIN values, bearer tokens and OTP
 * values are deliberately excluded.</p>
 *
 * <p>Functional classification: {@code claimType},
 * {@code taxpayerIdentifier} and {@code beneficiaries} describe the
 * Treasury-payment capability; {@code externalSubscriptionReference} is
 * opaque provider trace metadata; {@code applicationId} is partner
 * application metadata. None of those five values is promoted to a universal
 * Payment invariant by this command.</p>
 */
public record InitiatePaymentCommand(
        PaymentSource source,
        ExternalSubscriptionReference externalSubscriptionReference,
        CanonicalPartnerIdentity partnerIdentity,
        String applicationId,
        String endToEndId,
        BigDecimal totalAmount,
        String currency,
        String debtorRib,
        String debtorName,
        ClaimType claimType,
        String taxpayerIdentifier,
        Instant requestedExecutionAt,
        List<PaymentBeneficiaryCommand> beneficiaries,
        String callbackUrl,
        String idempotencyKey,
        CorrelationId correlationId
) {

    private static final int MAXIMUM_BENEFICIARIES = 20;

    public InitiatePaymentCommand {
        source = Objects.requireNonNull(
                source,
                "Payment source"
        );
        externalSubscriptionReference = Objects.requireNonNull(
                externalSubscriptionReference,
                "External subscription reference"
        );
        partnerIdentity = Objects.requireNonNull(
                partnerIdentity,
                "Canonical Partner identity"
        );
        applicationId = normalizeOptional(
                applicationId,
                64,
                "Application ID"
        );
        endToEndId = requireText(
                endToEndId,
                128,
                "End-to-end ID"
        );
        totalAmount = Objects.requireNonNull(
                totalAmount,
                "Total amount"
        );
        currency = requireText(
                currency,
                3,
                "Currency"
        ).toUpperCase(java.util.Locale.ROOT);
        debtorRib = requireText(
                debtorRib,
                64,
                "Debtor RIB"
        );
        debtorName = requireText(
                debtorName,
                200,
                "Debtor name"
        );
        claimType = Objects.requireNonNull(
                claimType,
                "Claim type"
        );
        taxpayerIdentifier = requireText(
                taxpayerIdentifier,
                64,
                "Taxpayer identifier"
        );
        requestedExecutionAt = Objects.requireNonNull(
                requestedExecutionAt,
                "Requested execution instant"
        );
        callbackUrl = requireText(
                callbackUrl,
                2048,
                "Callback URL"
        );
        idempotencyKey = requireText(
                idempotencyKey,
                128,
                "Idempotency key"
        );
        correlationId = Objects.requireNonNull(
                correlationId,
                "Correlation ID"
        );

        if (totalAmount.signum() <= 0) {
            throw new IllegalArgumentException(
                    "Total amount must be positive"
            );
        }

        if (totalAmount.scale() > 2) {
            throw new IllegalArgumentException(
                    "Total amount must have at most 2 decimals"
            );
        }

        if (!currency.matches("^[A-Z]{3}$")) {
            throw new IllegalArgumentException(
                    "Currency must use an ISO 4217 alpha-3 code"
            );
        }

        if (!callbackUrl.startsWith("https://")) {
            throw new IllegalArgumentException(
                    "Callback URL must use HTTPS"
            );
        }

        beneficiaries = List.copyOf(
                Objects.requireNonNull(
                        beneficiaries,
                        "Beneficiaries"
                )
        );

        if (beneficiaries.isEmpty()
                || beneficiaries.size()
                > MAXIMUM_BENEFICIARIES) {
            throw new IllegalArgumentException(
                    "Beneficiaries must contain between 1 and "
                            + MAXIMUM_BENEFICIARIES
                            + " entries"
            );
        }

        BigDecimal allocatedAmount =
                beneficiaries.stream()
                        .map(
                                PaymentBeneficiaryCommand
                                        ::amount
                        )
                        .reduce(
                                BigDecimal.ZERO,
                                BigDecimal::add
                        );

        if (allocatedAmount.compareTo(totalAmount) != 0) {
            throw new IllegalArgumentException(
                    "Beneficiary amount sum must equal total amount"
            );
        }
    }

    private static String normalizeOptional(
            String value,
            int maximumLength,
            String label
    ) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return requireText(
                value,
                maximumLength,
                label
        );
    }

    private static String requireText(
            String value,
            int maximumLength,
            String label
    ) {
        if (value == null) {
            throw new IllegalArgumentException(
                    label + " must not be null"
            );
        }

        String normalized = value.trim();

        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(
                    label + " must not be blank"
            );
        }

        if (normalized.length() > maximumLength) {
            throw new IllegalArgumentException(
                    label + " exceeds "
                            + maximumLength
                            + " characters"
            );
        }

        return normalized;
    }
}
