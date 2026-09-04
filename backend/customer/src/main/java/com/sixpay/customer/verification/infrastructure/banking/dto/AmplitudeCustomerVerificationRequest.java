package com.sixpay.customer.verification.infrastructure.banking.dto;

import java.time.Instant;
import java.util.List;

public record AmplitudeCustomerVerificationRequest(
        String financialInstitutionCode,
        AmplitudeCustomerVerificationSubject customer,
        AmplitudeAccountVerificationSubject account,
        List<String> requiredKycFields,
        Instant requestedAt
) {
    public AmplitudeCustomerVerificationRequest(
            String accountReference,
            String expectedNiu,
            String expectedAccountHolder,
            String financialInstitutionCode
    ) {
        this(
                financialInstitutionCode,
                new AmplitudeCustomerVerificationSubject(null, null, expectedNiu, expectedAccountHolder, null, null),
                new AmplitudeAccountVerificationSubject(accountReference, null, null),
                List.of("niu", "legalName", "phoneNumber", "email"),
                null
        );
    }

    @Override
    public String toString() {
        return "AmplitudeCustomerVerificationRequest["
                + "financialInstitutionCode=" + financialInstitutionCode
                + ", customer=[PROTECTED]"
                + ", account=[PROTECTED]"
                + ", requiredKycFields=" + requiredKycFields
                + ", requestedAt=" + requestedAt
                + "]";
    }
}
