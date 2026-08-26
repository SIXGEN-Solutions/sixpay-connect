package com.sixpay.payment.domain.model;

import com.sixpay.sharedkernel.domain.valueobject.ValueObject;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Immutable non-secret business context supplied by TresorPay at initiation.
 *
 * <p>API keys, PIN values, access tokens and OTP values are deliberately
 * excluded from this value object.</p>
 */
public record PaymentInitiationContext(
        String partnerLoginName,
        String applicationId,
        String debtorName,
        ClaimType claimType,
        String taxpayerIdentifier,
        Instant requestedExecutionAt,
        CallbackEndpoint callbackEndpoint
) implements ValueObject {

    private static final Pattern IDENTIFIER =
            Pattern.compile("^[A-Za-z0-9][A-Za-z0-9._-]{0,63}$");

    public PaymentInitiationContext {
        partnerLoginName = requireIdentifier(
                partnerLoginName,
                "Partner login name"
        );
        applicationId = normalizeOptionalIdentifier(applicationId);
        debtorName = requireText(debtorName, 200, "Debtor name");
        claimType = Objects.requireNonNull(claimType, "Claim type");
        taxpayerIdentifier = requireText(
                taxpayerIdentifier,
                64,
                "Taxpayer identifier"
        );
        requestedExecutionAt = Objects.requireNonNull(
                requestedExecutionAt,
                "Requested execution instant"
        );
        callbackEndpoint = Objects.requireNonNull(
                callbackEndpoint,
                "Callback endpoint"
        );
    }

    public Optional<String> optionalApplicationId() {
        return Optional.ofNullable(applicationId);
    }

    private static String requireIdentifier(
            String value,
            String label
    ) {
        String normalized = requireText(value, 64, label);
        if (!IDENTIFIER.matcher(normalized).matches()) {
            throw new IllegalArgumentException(
                    label + " has an invalid format"
            );
        }
        return normalized;
    }

    private static String normalizeOptionalIdentifier(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return requireIdentifier(value, "Application ID");
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
                    label + " exceeds " + maximumLength + " characters"
            );
        }
        return normalized;
    }

    @Override
    public String toString() {
        return "PaymentInitiationContext[partnerLoginName="
                + partnerLoginName
                + ", applicationIdPresent="
                + (applicationId != null)
                + ", claimType="
                + claimType
                + ", requestedExecutionAt="
                + requestedExecutionAt
                + ", callbackConfigured=true]";
    }
}
