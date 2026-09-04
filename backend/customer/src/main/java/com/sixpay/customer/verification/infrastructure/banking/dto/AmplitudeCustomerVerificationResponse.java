package com.sixpay.customer.verification.infrastructure.banking.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AmplitudeCustomerVerificationResponse(
        UUID verificationId,
        Instant verifiedAt,
        String source,
        String outcome,
        String customerReference,
        String accountReference,
        List<AmplitudeVerificationCheckResponse> checks,
        AmplitudeCustomerIdentityResponse identity,
        AmplitudeBankAccountResponse account
) {
    @Override
    public String toString() {
        return "AmplitudeCustomerVerificationResponse["
                + "verificationId=" + verificationId
                + ", verifiedAt=" + verifiedAt
                + ", source=" + source
                + ", outcome=" + outcome
                + ", customerReference=[PROTECTED]"
                + ", accountReference=[PROTECTED]"
                + ", checks=" + checks
                + ", identity=[PROTECTED]"
                + ", account=[PROTECTED]"
                + "]";
    }
}
