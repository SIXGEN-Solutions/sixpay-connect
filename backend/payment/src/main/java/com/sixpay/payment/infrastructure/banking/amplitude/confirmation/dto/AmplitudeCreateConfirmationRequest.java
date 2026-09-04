package com.sixpay.payment.infrastructure.banking.amplitude.confirmation.dto;

import java.math.BigDecimal;

public record AmplitudeCreateConfirmationRequest(
        String paymentReference,
        String customerReference,
        String debtorAccountReference,
        Money amount
) {
    public record Money(BigDecimal amount, String currency) { }
}
