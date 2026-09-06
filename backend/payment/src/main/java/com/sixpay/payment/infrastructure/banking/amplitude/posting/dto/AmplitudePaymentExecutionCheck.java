package com.sixpay.payment.infrastructure.banking.amplitude.posting.dto;

public record AmplitudePaymentExecutionCheck(
        String type,
        String result,
        String reasonCode
) { }
