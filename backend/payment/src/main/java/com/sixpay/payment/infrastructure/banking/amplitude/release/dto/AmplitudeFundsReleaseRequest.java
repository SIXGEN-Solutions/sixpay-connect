package com.sixpay.payment.infrastructure.banking.amplitude.release.dto;

public record AmplitudeFundsReleaseRequest(
        String paymentId,
        String reservationReference,
        String reasonCode,
        String financialInstitutionCode
) { }
