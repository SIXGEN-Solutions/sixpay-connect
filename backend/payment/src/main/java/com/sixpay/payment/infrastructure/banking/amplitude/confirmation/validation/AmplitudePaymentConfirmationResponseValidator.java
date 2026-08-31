package com.sixpay.payment.infrastructure.banking.amplitude.confirmation.validation;

import com.sixpay.payment.infrastructure.banking.amplitude.confirmation.dto.AmplitudeConfirmationResponse;

import java.util.Objects;
import java.util.Set;

public final class AmplitudePaymentConfirmationResponseValidator {
    private static final Set<String> STATUSES = Set.of(
            "ACTIVE", "VERIFIED", "EXPIRED", "LOCKED", "REPLACED", "REVOKED"
    );
    private static final Set<String> BUSINESS_CODES = Set.of(
            "CHALLENGE_ACTIVE", "OTP_VERIFIED", "OTP_INVALID",
            "CHALLENGE_EXPIRED", "CHALLENGE_LOCKED", "CHALLENGE_REPLACED",
            "CHALLENGE_REVOKED", "RESEND_NOT_ALLOWED",
            "CONFIRMATION_NOT_AVAILABLE", "DEPENDENCY_RESULT_UNKNOWN",
            "DELIVERY_FAILED"
    );

    public AmplitudeConfirmationResponse validate(AmplitudeConfirmationResponse response) {
        Objects.requireNonNull(response, "Confirmation response");
        requireText(response.paymentReference(), "paymentReference");
        requireText(response.challengeReference(), "challengeReference");
        requireText(response.challengeStatus(), "challengeStatus");
        requireText(response.businessCode(), "businessCode");
        if (!STATUSES.contains(response.challengeStatus())) {
            throw new IllegalStateException("Unsupported confirmation challenge status");
        }
        if (!BUSINESS_CODES.contains(response.businessCode())) {
            throw new IllegalStateException("Unsupported confirmation business code");
        }
        if ("VERIFIED".equals(response.challengeStatus()) && response.verifiedAt() == null) {
            throw new IllegalStateException("VERIFIED confirmation requires verifiedAt");
        }
        if (response.sentAt() != null && response.expiresAt() != null
                && response.expiresAt().isBefore(response.sentAt())) {
            throw new IllegalStateException("expiresAt must not be before sentAt");
        }
        return response;
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(field + " is required");
        }
    }
}
