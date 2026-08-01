package com.sixpay.payment.infrastructure.outbox;

import com.sixpay.common.context.CorrelationId;
import com.sixpay.payment.domain.event.PaymentDomainEvent;
import com.sixpay.payment.domain.event.PaymentEventMetadata;
import com.sixpay.payment.domain.model.PaymentId;
import com.sixpay.payment.domain.model.PaymentStatus;
import com.sixpay.payment.domain.model.PublicPaymentReference;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PaymentDomainEventMapperTest {

    @Test
    void mapsDomainEventToPendingOutboxEntity() throws Exception {
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        PaymentDomainEvent event = event();
        Instant createdAt = Instant.parse("2026-08-01T16:00:01Z");

        when(objectMapper.writeValueAsString(event))
                .thenReturn("{\"event\":\"received\"}");

        PaymentOutboxEntity entity =
                new PaymentDomainEventMapper(objectMapper)
                        .toOutboxEntity(event, createdAt);

        assertThat(entity.eventId()).isEqualTo(event.eventId());
        assertThat(entity.aggregateType()).isEqualTo("PAYMENT");
        assertThat(entity.aggregateId()).isEqualTo(event.paymentId().value());
        assertThat(entity.eventType()).isEqualTo("TestPaymentEvent");
        assertThat(entity.schemaVersion()).isOne();
        assertThat(entity.correlationId())
                .isEqualTo(event.correlationId().value());
        assertThat(entity.payload())
                .isEqualTo("{\"event\":\"received\"}");
        assertThat(entity.status())
                .isEqualTo(PaymentOutboxEntity.Status.PENDING);
        assertThat(entity.attemptCount()).isZero();
        assertThat(entity.nextAttemptAt()).isEqualTo(createdAt);
    }

    private static PaymentDomainEvent event() {
        UUID paymentId = UUID.randomUUID();
        return new TestPaymentEvent(
                new PaymentEventMetadata(
                        UUID.randomUUID(),
                        new PaymentId(paymentId),
                        PublicPaymentReference.of(reference(paymentId)),
                        CorrelationId.of("corr-" + paymentId),
                        PaymentStatus.RECEIVED,
                        1L,
                        1,
                        null,
                        Instant.parse("2026-08-01T16:00:00Z")
                )
        );
    }

    private static String reference(UUID paymentId) {
        return "PAY-" + paymentId.toString()
                .replace("-", "")
                .substring(0, 26)
                .toUpperCase();
    }

    private record TestPaymentEvent(
            PaymentEventMetadata metadata
    ) implements PaymentDomainEvent {
    }
}
