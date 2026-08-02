package com.sixpay.payment.infrastructure.callback.relay;

import java.time.Instant;
import java.util.UUID;

record ClaimedPaymentOutboxEvent(
        UUID eventId,
        UUID paymentId,
        String eventType,
        String correlationId,
        Instant occurredAt,
        int attemptCount
) {
}
