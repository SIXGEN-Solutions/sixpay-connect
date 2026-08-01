package com.sixpay.payment.infrastructure.audit;

import com.sixpay.common.context.CorrelationId;
import com.sixpay.payment.domain.event.PaymentDomainEvent;
import com.sixpay.payment.domain.event.PaymentEventMetadata;
import com.sixpay.payment.domain.model.PaymentId;
import com.sixpay.payment.domain.model.PaymentStatus;
import com.sixpay.payment.domain.model.PublicPaymentReference;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PaymentAuditEntityTest {

    @Test
    void mapsOnlySafeDomainEventMetadata() {
        UUID eventId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        Instant occurredAt = Instant.parse("2026-08-01T12:00:00Z");

        PaymentDomainEvent event = new TestPaymentEvent(
                new PaymentEventMetadata(
                        eventId,
                        new PaymentId(paymentId),
                        PublicPaymentReference.of("PAY-00000000000000000000000000"),
                        CorrelationId.of("correlation-1"),
                        PaymentStatus.RECEIVED,
                        1L,
                        1,
                        null,
                        occurredAt
                )
        );

        PaymentAuditEntity entity = PaymentAuditEntity.from(event);

        assertEquals(eventId, entity.eventId());
        assertEquals(paymentId, entity.paymentId());
        assertEquals("TestPaymentEvent", entity.eventType());
        assertEquals(PaymentStatus.RECEIVED, entity.paymentStatus());
        assertEquals(1L, entity.businessVersion());
        assertEquals(1, entity.eventSequence());
        assertEquals("correlation-1", entity.correlationId());
        assertNull(entity.causationId());
        assertEquals(occurredAt, entity.occurredAt());
    }

    private record TestPaymentEvent(PaymentEventMetadata metadata)
            implements PaymentDomainEvent {}
}
