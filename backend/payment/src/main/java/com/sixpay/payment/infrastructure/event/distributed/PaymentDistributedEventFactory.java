package com.sixpay.payment.infrastructure.event.distributed;

import com.sixpay.integration.event.DistributedEventEnvelope;
import com.sixpay.integration.event.PayloadClassification;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class PaymentDistributedEventFactory {

    public DistributedEventEnvelope<Object> create(
            UUID eventId,
            String eventType,
            String paymentId,
            String correlationId,
            String causationId,
            Instant occurredAt,
            Object payload
    ) {
        Objects.requireNonNull(paymentId, "paymentId");

        return new DistributedEventEnvelope<>(
                eventId,
                eventType,
                1,
                occurredAt,
                "payment",
                "Payment",
                paymentId,
                correlationId,
                causationId,
                paymentId,
                PayloadClassification.INTERNAL,
                payload,
                Map.of(
                        "contractOwner",
                        "payment",
                        "partitionStrategy",
                        "paymentId"
                )
        );
    }
}
