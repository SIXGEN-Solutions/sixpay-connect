package com.sixpay.payment.infrastructure.outbox;

import com.sixpay.payment.domain.event.PaymentDomainEvent;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.Objects;

/**
 * Legacy mapper for the canonical Payment domain-event outbox contract.
 *
 * <p>The persisted event type is a stable logical identifier and never the
 * concrete Java class name. Observed Customer projection events are created
 * separately through PaymentObservedCustomerProjectionEventMapper.</p>
 */
@Component
public final class PaymentDomainEventMapper {

    static final int CURRENT_SCHEMA_VERSION = 1;

    static final String PAYMENT_DOMAIN_EVENT_TYPE =
            "payment.domain-event";

    private final ObjectMapper objectMapper;

    public PaymentDomainEventMapper(
            ObjectMapper objectMapper
    ) {
        this.objectMapper = Objects.requireNonNull(
                objectMapper,
                "Object mapper is required"
        );
    }

    public PaymentOutboxEntity toOutboxEntity(
            PaymentDomainEvent event,
            Instant createdAt
    ) {
        Objects.requireNonNull(
                event,
                "Payment domain event is required"
        );

        Objects.requireNonNull(
                createdAt,
                "Outbox creation instant is required"
        );

        if (createdAt.isBefore(event.occurredAt())) {
            throw new IllegalArgumentException(
                    "Outbox creation instant must not precede "
                            + "the event occurrence instant"
            );
        }

        return PaymentOutboxEntity.create(
                event.eventId(),
                event.paymentId().value(),
                PAYMENT_DOMAIN_EVENT_TYPE,
                CURRENT_SCHEMA_VERSION,
                event.correlationId().value(),
                serialize(event),
                event.occurredAt(),
                createdAt
        );
    }

    private String serialize(
            PaymentDomainEvent event
    ) {
        try {
            return objectMapper.writeValueAsString(
                    event
            );
        } catch (JacksonException exception) {
            throw new PaymentOutboxMappingException(
                    "Cannot serialize Payment domain event "
                            + event.eventId(),
                    exception
            );
        }
    }
}