package com.sixpay.customer.verification.application.port.output;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Immutable Customer-owned projection of the canonical debtor account returned
 * by authoritative Core Banking system after Customer Verification.
 */
public record VerifiedBankingAccount(
        String accountReference,
        String customerReference,
        String financialInstitutionCode,
        String maskedAccountIdentifier,
        String currency,
        String accountType,
        String status,
        List<String> restrictions,
        Instant retrievedAt
) {
    public VerifiedBankingAccount {
        accountReference = required(accountReference, "accountReference");
        customerReference = required(customerReference, "customerReference");
        financialInstitutionCode = required(
                financialInstitutionCode,
                "financialInstitutionCode"
        );
        maskedAccountIdentifier = required(
                maskedAccountIdentifier,
                "maskedAccountIdentifier"
        );
        currency = required(currency, "currency");
        accountType = required(accountType, "accountType");
        status = required(status, "status");
        restrictions = List.copyOf(
                Objects.requireNonNull(restrictions, "restrictions are required")
        );
        retrievedAt = Objects.requireNonNull(
                retrievedAt,
                "retrievedAt is required"
        );
    }

    @Override
    public String toString() {
        return "VerifiedBankingAccount[accountReference=[PROTECTED]"
                + ", customerReference=[PROTECTED]"
                + ", financialInstitutionCode=" + financialInstitutionCode
                + ", maskedAccountIdentifier=[PROTECTED]"
                + ", currency=" + currency
                + ", accountType=" + accountType
                + ", status=" + status
                + ", restrictions=" + restrictions
                + ", retrievedAt=" + retrievedAt
                + "]";
    }

    private static String required(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " is required");
        }
        return value.strip();
    }
}
