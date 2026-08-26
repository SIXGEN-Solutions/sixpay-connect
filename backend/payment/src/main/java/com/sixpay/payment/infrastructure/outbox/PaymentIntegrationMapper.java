package com.sixpay.payment.infrastructure.outbox;

import com.sixpay.common.messaging.model.IntegrationEventEnvelope;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public final class PaymentIntegrationMapper {

    public IntegrationEventEnvelope toEnvelope(
            PaymentOutboxEntity entity
    ) {
        Objects.requireNonNull(entity, "Payment outbox entity");

        return new IntegrationEventEnvelope(
                entity.eventId(),
                entity.eventType(),
                entity.schemaVersion(),
                entity.aggregateType(),
                entity.aggregateId(),
                entity.correlationId(),
                entity.occurredAt(),
                entity.payload()
        );
    }
}
