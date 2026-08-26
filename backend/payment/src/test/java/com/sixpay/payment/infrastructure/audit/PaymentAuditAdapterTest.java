package com.sixpay.payment.infrastructure.audit;

import com.sixpay.common.context.CorrelationId;
import com.sixpay.payment.domain.event.PaymentDomainEvent;
import com.sixpay.payment.domain.event.PaymentEventMetadata;
import com.sixpay.payment.domain.model.PaymentId;
import com.sixpay.payment.domain.model.PaymentStatus;
import com.sixpay.payment.domain.model.PublicPaymentReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentAuditAdapterTest {

    private static final Instant OCCURRED_AT =
            Instant.parse("2026-08-01T12:00:00Z");

    private PaymentAuditRepository repository;
    private PaymentAuditAdapter adapter;

    @BeforeEach
    void setUp() {
        repository = Mockito.mock(
                PaymentAuditRepository.class
        );

        adapter = new PaymentAuditAdapter(repository);
    }

    @Test
    void appendsOneNewDomainEvent() {
        PaymentDomainEvent event = paymentEvent();

        when(
                repository.existsByEventId(event.eventId())
        ).thenReturn(false);

        adapter.append(event);

        ArgumentCaptor<PaymentAuditEntity> captor =
                ArgumentCaptor.forClass(
                        PaymentAuditEntity.class
                );

        verify(repository).save(captor.capture());

        PaymentAuditEntity persisted = captor.getValue();

        assertThat(persisted.eventId())
                .isEqualTo(event.eventId());

        assertThat(persisted.paymentId())
                .isEqualTo(
                        event.paymentId().value()
                );

        assertThat(persisted.publicPaymentReference())
                .isEqualTo(
                        event.paymentReference().value()
                );

        assertThat(persisted.eventType())
                .isEqualTo("TestPaymentEvent");

        assertThat(persisted.paymentStatus())
                .isEqualTo(event.paymentStatus());

        assertThat(persisted.businessVersion())
                .isEqualTo(event.aggregateVersion());

        assertThat(persisted.eventSequence())
                .isEqualTo(event.eventSequence());

        assertThat(persisted.correlationId())
                .isEqualTo(
                        event.correlationId().value()
                );

        assertThat(persisted.causationId())
                .isEqualTo(event.causationId());

        assertThat(persisted.occurredAt())
                .isEqualTo(event.occurredAt());
    }

    @Test
    void identicalEventReplayIsANoOp() {
        PaymentDomainEvent event = paymentEvent();

        when(
                repository.existsByEventId(event.eventId())
        ).thenReturn(true);

        adapter.append(event);

        verify(repository, never())
                .save(Mockito.any(PaymentAuditEntity.class));
    }

    private static PaymentDomainEvent paymentEvent() {
        UUID eventId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();

        PaymentEventMetadata metadata =
                new PaymentEventMetadata(
                        eventId,
                        new PaymentId(paymentId),
                        PublicPaymentReference.of(
                                publicPaymentReference(paymentId)
                        ),
                        CorrelationId.of(
                                "correlation-" + paymentId
                        ),
                        PaymentStatus.RECEIVED,
                        1L,
                        1,
                        null,
                        OCCURRED_AT
                );

        return new TestPaymentEvent(metadata);
    }

    private static String publicPaymentReference(
            UUID paymentId
    ) {
        String identifier = paymentId
                .toString()
                .replace("-", "")
                .substring(0, 26)
                .toUpperCase();

        return "PAY-" + identifier;
    }

    private record TestPaymentEvent(
            PaymentEventMetadata metadata
    ) implements PaymentDomainEvent {
    }
}