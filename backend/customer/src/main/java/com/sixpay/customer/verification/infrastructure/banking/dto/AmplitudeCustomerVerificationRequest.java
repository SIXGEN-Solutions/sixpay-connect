package com.sixpay.customer.verification.infrastructure.banking.dto;

public record AmplitudeCustomerVerificationRequest(
        String accountReference,
        String expectedNiu,
        String expectedAccountHolder,
        String financialInstitutionCode
) {
    @Override
    public String toString() {
        return "AmplitudeCustomerVerificationRequest["
                + "accountReference=[PROTECTED], "
                + "expectedNiu=[PROTECTED], "
                + "expectedAccountHolder=[PROTECTED], "
                + "financialInstitutionCode=" + financialInstitutionCode
                + "]";
    }
}
