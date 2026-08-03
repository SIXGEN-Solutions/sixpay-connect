package com.sixpay.payment.application.port.output;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Payment-native request for Customer Verification.
 *
 * <p>The integration account token and binding fingerprint are kept separate:
 * the first addresses the account through Customer infrastructure; the second
 * binds returned evidence to the Payment aggregate.</p>
 */
public record CustomerVerificationRequest(
        UUID verificationId,
        String customerNiu,
        String customerLegalName,
        String financialInstitutionCode,
        String accountBindingFingerprint,
        String integrationAccountToken,
        String correlationId,
        UUID causationId,
        Instant requestedAt
) {

    public CustomerVerificationRequest {
        verificationId = Objects.requireNonNull(
                verificationId,
                "verificationId is required"
        );
        customerNiu = requireText(customerNiu, "customerNiu");
        customerLegalName = requireText(
                customerLegalName,
                "customerLegalName"
        );
        financialInstitutionCode = requireText(
                financialInstitutionCode,
                "financialInstitutionCode"
        );
        accountBindingFingerprint = requireText(
                accountBindingFingerprint,
                "accountBindingFingerprint"
        );
        integrationAccountToken = requireText(
                integrationAccountToken,
                "integrationAccountToken"
        );
        correlationId = requireText(
                correlationId,
                "correlationId"
        );
        requestedAt = Objects.requireNonNull(
                requestedAt,
                "requestedAt is required"
        );
    }

    private static String requireText(
            String value,
            String name
    ) {
        Objects.requireNonNull(value, name + " is required");

        String normalized = value.strip();

        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(
                    name + " must not be blank"
            );
        }

        return normalized;
    }

    @Override
    public String toString() {
        return "CustomerVerificationRequest[verificationId="
                + verificationId
                + ", customerNiu=[PROTECTED]"
                + ", customerLegalName=[PROTECTED]"
                + ", financialInstitutionCode="
                + financialInstitutionCode
                + ", accountBindingFingerprint=[PROTECTED]"
                + ", integrationAccountToken=[PROTECTED]"
                + ", correlationId="
                + correlationId
                + ", causationId="
                + causationId
                + ", requestedAt="
                + requestedAt
                + "]";
    }
}
