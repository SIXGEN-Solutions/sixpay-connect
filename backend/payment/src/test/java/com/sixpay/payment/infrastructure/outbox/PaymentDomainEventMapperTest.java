package com.sixpay.payment.infrastructure.outbox;

import com.sixpay.common.context.CorrelationId;
import com.sixpay.payment.domain.event.PaymentDomainEvent;
import com.sixpay.payment.domain.event.PaymentEventMetadata;
import com.sixpay.payment.domain.model.PaymentId;
import com.sixpay.payment.domain.model.PaymentStatus;
import com.sixpay.payment.domain.model.PublicPaymentReference;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentDomainEventMapperTest {

    private static final UUID EVENT_ID =
            UUID.fromString(
                    "11111111-1111-4111-8111-111111111111"
            );

    private static final UUID PAYMENT_ID =
            UUID.fromString(
                    "7ed75090-8af7-4dfa-9b62-8e4dca73501a"
            );

    private static final String CORRELATION_ID =
            "c74e165f-df46-463e-a520-188e6df3e5ae";

    private static final Instant OCCURRED_AT =
            Instant.parse(
                    "2026-08-04T18:00:00Z"
            );

    private static final Instant CREATED_AT =
            OCCURRED_AT.plusSeconds(1);

    @Test
    void mapsDomainEventToPendingOutboxEntity() {
        ObjectMapper objectMapper =
                JsonMapper.builder()
                        .findAndAddModules()
                        .build();

        PaymentDomainEventMapper mapper =
                new PaymentDomainEventMapper(
                        objectMapper
                );

        PaymentEventMetadata metadata =
                new PaymentEventMetadata(
                        EVENT_ID,
                        new PaymentId(PAYMENT_ID),
                        PublicPaymentReference.of(
                                "PAY-0123456789ABCDEFGHJKMNPQRS"
                        ),
                        CorrelationId.of(
                                CORRELATION_ID
                        ),
                        PaymentStatus.RECEIVED,
                        1L,
                        1,
                        null,
                        OCCURRED_AT
                );

        PaymentDomainEvent event =
                new TestPaymentDomainEvent(
                        metadata
                );

        PaymentOutboxEntity entity =
                mapper.toOutboxEntity(
                        event,
                        CREATED_AT
                );

        assertThat(entity.eventId())
                .isEqualTo(EVENT_ID);

        assertThat(entity.aggregateId())
                .isEqualTo(PAYMENT_ID);

        assertThat(entity.aggregateType())
                .isEqualTo("PAYMENT");

        assertThat(entity.eventType())
                .isEqualTo(
                        PaymentDomainEventMapper
                                .PAYMENT_DOMAIN_EVENT_TYPE
                );

        assertThat(entity.eventType())
                .isEqualTo(
                        "payment.domain-event"
                );

        assertThat(entity.schemaVersion())
                .isEqualTo(
                        PaymentDomainEventMapper
                                .CURRENT_SCHEMA_VERSION
                );

        assertThat(entity.correlationId())
                .isEqualTo(CORRELATION_ID);

        assertThat(entity.occurredAt())
                .isEqualTo(OCCURRED_AT);

        assertThat(entity.createdAt())
                .isEqualTo(CREATED_AT);

        assertThat(entity.status())
                .isEqualTo(
                        PaymentOutboxEntity.Status.PENDING
                );

        assertThat(entity.attemptCount())
                .isZero();

        assertThat(entity.nextAttemptAt())
                .isEqualTo(CREATED_AT);

        assertThat(entity.payload())
                .contains(
                        EVENT_ID.toString()
                );

        assertThat(entity.payload())
                .contains(
                        PAYMENT_ID.toString()
                );

        assertThat(entity.payload())
                .doesNotContain(
                        "mockitoInterceptor"
                );
    }

    private record TestPaymentDomainEvent(
            PaymentEventMetadata metadata
    ) implements PaymentDomainEvent {
    }
}