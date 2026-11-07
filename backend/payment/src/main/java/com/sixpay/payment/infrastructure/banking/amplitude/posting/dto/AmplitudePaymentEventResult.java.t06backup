package com.sixpay.payment.infrastructure.banking.amplitude.posting.dto;

import java.time.Instant;
import java.util.List;

public record AmplitudePaymentEventResult(
        String paymentReference,
        String outcome,
        List<AmplitudePaymentExecutionCheck> checks,
        String bankReference,
        String reasonCode,
        Instant observedAt
) { }
