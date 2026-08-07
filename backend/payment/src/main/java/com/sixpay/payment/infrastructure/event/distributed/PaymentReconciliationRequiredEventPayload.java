package com.sixpay.payment.infrastructure.event.distributed;

import java.time.Instant;

public record PaymentReconciliationRequiredEventPayload(
        String paymentId,
        String publicPaymentReference,
        String reasonCode,
        String nextAction,
        String bankPostingReference,
        Instant detectedAt
) { }
