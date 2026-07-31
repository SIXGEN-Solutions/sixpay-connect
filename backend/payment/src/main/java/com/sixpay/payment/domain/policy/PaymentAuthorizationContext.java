package com.sixpay.payment.domain.policy;

import com.sixpay.payment.domain.model.ExternalPaymentReference;
import com.sixpay.payment.domain.model.ExternalSubscriptionReference;
import com.sixpay.payment.domain.model.FinancialInstitutionCode;

import java.util.Objects;

public record PaymentAuthorizationContext(
        ExternalSubscriptionReference subscriptionReference,
        ExternalPaymentReference externalPaymentReference,
        FinancialInstitutionCode financialInstitutionCode,
        String accountBindingFingerprint
) {
    public PaymentAuthorizationContext {
        subscriptionReference = Objects.requireNonNull(
                subscriptionReference,
                "Subscription reference"
        );
        externalPaymentReference = Objects.requireNonNull(
                externalPaymentReference,
                "External Payment reference"
        );
        financialInstitutionCode = Objects.requireNonNull(
                financialInstitutionCode,
                "Financial institution code"
        );
        accountBindingFingerprint = requireFingerprint(
                accountBindingFingerprint
        );
    }

    private static String requireFingerprint(String value) {
        Objects.requireNonNull(value, "Account binding fingerprint");
        if (!value.matches("^v1:[0-9a-f]{64}$")) {
            throw new IllegalArgumentException(
                    "Account binding fingerprint has an invalid format"
            );
        }
        return value;
    }
}
