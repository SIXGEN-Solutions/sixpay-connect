package com.sixpay.payment.domain.policy;

import com.sixpay.payment.domain.model.FinancialInstitutionCode;
import com.sixpay.sharedkernel.domain.valueobject.Money;

import java.util.Objects;

public record PaymentFundsContext(
        FinancialInstitutionCode financialInstitutionCode,
        String accountBindingFingerprint,
        Money amount
) {
    public PaymentFundsContext {
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
        amount = Objects.requireNonNull(amount, "Payment amount");
        if (!amount.isPositive()) {
            throw new IllegalArgumentException(
                    "Payment amount must be positive"
            );
        }
    }
}
