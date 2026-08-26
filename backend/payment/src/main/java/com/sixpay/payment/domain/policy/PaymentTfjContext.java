package com.sixpay.payment.domain.policy;

import com.sixpay.payment.domain.model.FinancialInstitutionCode;
import com.sixpay.payment.domain.model.PublicPaymentReference;

import java.time.LocalDate;
import java.util.Objects;

public record PaymentTfjContext(
        FinancialInstitutionCode financialInstitutionCode,
        LocalDate businessDate,
        PublicPaymentReference publicPaymentReference,
        String principalBankPostingReference
) {
    public PaymentTfjContext {
        financialInstitutionCode = Objects.requireNonNull(
                financialInstitutionCode,
                "Financial institution code"
        );
        businessDate = Objects.requireNonNull(
                businessDate,
                "Business date"
        );
        publicPaymentReference = Objects.requireNonNull(
                publicPaymentReference,
                "Public Payment reference"
        );
        Objects.requireNonNull(
                principalBankPostingReference,
                "Principal bank posting reference"
        );
    }
}
