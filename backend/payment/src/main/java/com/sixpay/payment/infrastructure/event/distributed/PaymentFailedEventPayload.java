package com.sixpay.payment.infrastructure.event.distributed;

import java.time.Instant;

public record PaymentFailedEventPayload(
        String paymentId,
        String publicPaymentReference,
        String failureCode,
        String finalStatus,
        Instant failedAt
) { }
