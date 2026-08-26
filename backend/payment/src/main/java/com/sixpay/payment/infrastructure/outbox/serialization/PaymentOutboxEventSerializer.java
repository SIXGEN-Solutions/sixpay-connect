package com.sixpay.payment.infrastructure.outbox.serialization;

import com.sixpay.payment.application.event.projection
        .ObservedCustomerProjectionEvent;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Objects;

/**
 * Serializes Payment projection events into a stable JSON envelope.
 */
public final class PaymentOutboxEventSerializer {

    private final ObjectMapper objectMapper;
    private final PaymentOutboxEventTypeRegistry registry;

    public PaymentOutboxEventSerializer(
            ObjectMapper objectMapper,
            PaymentOutboxEventTypeRegistry registry
    ) {
        this.objectMapper = Objects.requireNonNull(
                objectMapper,
                "objectMapper is required"
        );
        this.registry = Objects.requireNonNull(
                registry,
                "registry is required"
        );
    }

    public String serialize(
            ObservedCustomerProjectionEvent event
    ) {
        Objects.requireNonNull(
                event,
                "event is required"
        );

        PaymentOutboxEventTypeRegistry.ContractDescriptor contract =
                registry.observedCustomerProjection();

        try {
            JsonNode payload = objectMapper.valueToTree(
                    event.payload()
            );

            PaymentOutboxEventEnvelope envelope =
                    new PaymentOutboxEventEnvelope(
                            event.eventId(),
                            contract.eventType(),
                            contract.eventVersion(),
                            event.paymentId(),
                            event.aggregateType(),
                            event.aggregateVersion(),
                            event.eventType(),
                            event.correlationId(),
                            event.occurredAt(),
                            payload
                    );

            return objectMapper.writeValueAsString(
                    envelope
            );
        } catch (Exception exception) {
            throw new PaymentOutboxSerializationException(
                    "Cannot serialize Payment outbox event "
                            + event.eventId(),
                    exception
            );
        }
    }
}
