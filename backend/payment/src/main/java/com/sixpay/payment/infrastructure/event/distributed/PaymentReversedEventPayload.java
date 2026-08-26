package com.sixpay.payment.infrastructure.event.distributed;

import java.time.Instant;

public record PaymentReversedEventPayload(
        String paymentId,
        String publicPaymentReference,
        String reversalOutcome,
        String reversalReference,
        String reasonCode,
        Instant reversedAt
) { }
