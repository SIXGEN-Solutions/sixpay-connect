package com.sixpay.customer.verification.infrastructure.banking.dto;

public record AmplitudeCustomerVerificationSubject(
        String customerReference,
        String customerNumber,
        String niu,
        String legalName,
        String phoneNumber,
        String email
) { }
