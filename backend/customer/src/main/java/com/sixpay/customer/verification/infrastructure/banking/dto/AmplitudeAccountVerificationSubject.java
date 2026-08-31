package com.sixpay.customer.verification.infrastructure.banking.dto;

public record AmplitudeAccountVerificationSubject(
        String accountReference,
        String rib,
        String iban
) { }
