package com.sixpay.payment.domain.policy;

import com.sixpay.payment.domain.model.FinancialInstitutionCode;

import java.util.Objects;

public record PaymentBankingContext(
        FinancialInstitutionCode financialInstitutionCode,
        String accountBindingFingerprint
) {
    public PaymentBankingContext {
        financialInstitutionCode = Objects.requireNonNull(
                financialInstitutionCode,
                "Financial institution code"
        );
        Objects.requireNonNull(
                accountBindingFingerprint,
                "Account binding fingerprint"
        );
        if (!accountBindingFingerprint.matches("^v1:[0-9a-f]{64}$")) {
            throw new IllegalArgumentException(
                    "Account binding fingerprint has an invalid format"
            );
        }
    }
}
