package com.sixpay.payment.infrastructure.outbox;

import com.sixpay.payment.domain.event.PaymentDomainEvent;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.Objects;

@Component
public final class PaymentDomainEventMapper {

    static final int CURRENT_SCHEMA_VERSION = 1;

    private final ObjectMapper objectMapper;

    public PaymentDomainEventMapper(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "Object mapper");
    }

    public PaymentOutboxEntity toOutboxEntity(
            PaymentDomainEvent event,
            Instant createdAt
    ) {
        Objects.requireNonNull(event, "Payment domain event");
        Objects.requireNonNull(createdAt, "Outbox creation instant");

        return new PaymentOutboxEntity(
                event.eventId(),
                event.paymentId().value(),
                event.getClass().getSimpleName(),
                CURRENT_SCHEMA_VERSION,
                event.correlationId().value(),
                serialize(event),
                event.occurredAt(),
                createdAt
        );
    }

    private String serialize(PaymentDomainEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JacksonException exception) {
            throw new PaymentOutboxMappingException(
                    "Cannot serialize Payment domain event " + event.eventId(),
                    exception
            );
        }
    }
}
