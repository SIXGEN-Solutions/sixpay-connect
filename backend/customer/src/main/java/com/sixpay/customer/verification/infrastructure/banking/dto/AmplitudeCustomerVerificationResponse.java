package com.sixpay.customer.verification.infrastructure.banking.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

public record AmplitudeCustomerVerificationResponse(
        String code,
        Boolean accountFound,
        String accountStatus,
        String accountHolder,
        String accountReferenceMasked,
        String currency,
        BigDecimal availableBalance,
        BigDecimal accountBalance,
        Boolean canDebit,
        String description,
        String result,
        Instant observedAt,
        Instant validUntil,
        Map<String, String> checks
) {
    @Override
    public String toString() {
        return "AmplitudeCustomerVerificationResponse["
                + "code=" + code
                + ", accountFound=" + accountFound
                + ", accountStatus=" + accountStatus
                + ", accountHolder=[PROTECTED]"
                + ", accountReferenceMasked=[PROTECTED]"
                + ", currency=" + currency
                + ", availableBalance=[PROTECTED]"
                + ", accountBalance=[PROTECTED]"
                + ", canDebit=" + canDebit
                + ", result=" + result
                + ", observedAt=" + observedAt
                + ", validUntil=" + validUntil
                + ", checks=" + checks
                + "]";
    }
}
