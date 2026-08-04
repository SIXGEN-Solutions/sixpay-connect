package com.sixpay.payment.infrastructure.outbox.serialization;

import com.sixpay.payment.application.event.projection
        .ObservedCustomerProjectionEvent;
import com.sixpay.payment.application.event.projection
        .ObservedCustomerProjectionPayload;
import tools.jackson.databind.ObjectMapper;

import java.util.Objects;

/**
 * Deserializes stable Payment outbox envelopes without relying on Java class
 * names stored in JSON.
 */
public final class PaymentOutboxEventDeserializer {

    private final ObjectMapper objectMapper;
    private final PaymentOutboxEventTypeRegistry registry;

    public PaymentOutboxEventDeserializer(
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

    public ObservedCustomerProjectionEvent deserialize(
            String serializedEnvelope
    ) {
        if (serializedEnvelope == null
                || serializedEnvelope.isBlank()) {
            throw new PaymentOutboxSerializationException(
                    "Serialized Payment outbox envelope "
                            + "must not be blank"
            );
        }

        PaymentOutboxEventEnvelope envelope;

        try {
            envelope = objectMapper.readValue(
                    serializedEnvelope,
                    PaymentOutboxEventEnvelope.class
            );
        } catch (Exception exception) {
            throw new PaymentOutboxSerializationException(
                    "Cannot decode Payment outbox envelope",
                    exception
            );
        }

        registry.requireSupported(
                envelope.eventType(),
                envelope.eventVersion()
        );

        try {
            ObservedCustomerProjectionPayload payload =
                    objectMapper.treeToValue(
                            envelope.payload(),
                            ObservedCustomerProjectionPayload.class
                    );

            return new ObservedCustomerProjectionEvent(
                    envelope.eventId(),
                    envelope.eventVersion(),
                    envelope.aggregateId(),
                    envelope.aggregateType(),
                    envelope.aggregateVersion(),
                    envelope.projectionEventType(),
                    payload,
                    envelope.correlationId(),
                    envelope.occurredAt()
            );
        } catch (UnknownPaymentOutboxEventTypeException
                 | UnsupportedPaymentOutboxEventVersionException
                         exception) {
            throw exception;
        } catch (Exception exception) {
            throw new PaymentOutboxSerializationException(
                    "Cannot decode Payment outbox payload for "
                            + envelope.eventType()
                            + "@"
                            + envelope.eventVersion(),
                    exception
            );
        }
    }
}
