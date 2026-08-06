package com.sixpay.payment.infrastructure.banking.amplitude.dto;

public record AmplitudeAccountVerificationRequest(
        String paymentId,
        String customerIdentifier,
        String debtorAccountIdentifier,
        String financialInstitutionCode
) { }
