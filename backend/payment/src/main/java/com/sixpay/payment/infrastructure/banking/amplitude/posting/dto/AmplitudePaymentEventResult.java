package com.sixpay.payment.infrastructure.banking.amplitude.posting.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record AmplitudePaymentEventResult(
        String paymentReference,
        String outcome,
        List<AmplitudePaymentExecutionCheck> checks,
        String bankReference,
        String reasonCode,
        Instant observedAt,
        LocalDate accountingDate
) {
    // Existing direct-execution adapters and tests remain source-compatible.
    public AmplitudePaymentEventResult(
            String paymentReference, String outcome,
            List<AmplitudePaymentExecutionCheck> checks,
            String bankReference, String reasonCode, Instant observedAt
    ) {
        this(paymentReference, outcome, checks, bankReference, reasonCode, observedAt, null);
    }
}
