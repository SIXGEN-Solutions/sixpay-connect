package com.sixpay.payment.application.service;

import com.sixpay.common.time.TimeProvider;
import com.sixpay.payment.domain.event.PaymentDomainEvent;
import com.sixpay.payment.domain.model.Payment;
import com.sixpay.payment.domain.model.PaymentId;
import com.sixpay.payment.domain.repository.PaymentRepository;
import com.sixpay.payment.infrastructure.audit.PaymentAuditAdapter;
import com.sixpay.payment.infrastructure.outbox.PaymentDomainEventMapper;
import com.sixpay.payment.infrastructure.outbox.PaymentOutboxEntity;
import com.sixpay.payment.infrastructure.outbox.PaymentOutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentMutationCoordinatorTest {

    private PaymentRepository paymentRepository;
    private PaymentAuditAdapter auditAdapter;
    private PaymentOutboxRepository outboxRepository;
    private PaymentDomainEventMapper eventMapper;
    private PaymentMutationCoordinator coordinator;

    @BeforeEach
    void setUp() {
        paymentRepository = Mockito.mock(
                PaymentRepository.class
        );
        auditAdapter = Mockito.mock(
                PaymentAuditAdapter.class
        );
        outboxRepository = Mockito.mock(
                PaymentOutboxRepository.class
        );
        eventMapper = Mockito.mock(
                PaymentDomainEventMapper.class
        );

        TimeProvider timeProvider = () ->
                Instant.parse("2026-08-01T20:00:00Z");

        coordinator = new PaymentMutationCoordinator(
                paymentRepository,
                auditAdapter,
                outboxRepository,
                eventMapper,
                timeProvider
        );
    }

    @Test
    void persistsAggregateAuditAndOutboxAfterMutation() {
        PaymentId paymentId =
                new PaymentId(UUID.randomUUID());
        Payment payment = Mockito.mock(Payment.class);
        PaymentDomainEvent event =
                Mockito.mock(PaymentDomainEvent.class);
        PaymentOutboxEntity outbox =
                Mockito.mock(PaymentOutboxEntity.class);

        when(paymentRepository.findById(paymentId))
                .thenReturn(Optional.of(payment));
        when(payment.businessVersion())
                .thenReturn(1L, 2L);
        when(payment.domainEvents())
                .thenReturn(List.of(event));
        when(event.occurredAt())
                .thenReturn(
                        Instant.parse(
                                "2026-08-01T19:59:59Z"
                        )
                );
        when(eventMapper.toOutboxEntity(
                event,
                Instant.parse("2026-08-01T20:00:00Z")
        )).thenReturn(outbox);

        PaymentWorkflowResult result =
                coordinator.mutate(
                        paymentId,
                        ignored -> {
                        }
                );

        assertThat(result.stateChanged()).isTrue();

        verify(paymentRepository).save(payment);
        verify(auditAdapter).appendAll(List.of(event));
        verify(outboxRepository).saveAll(List.of(outbox));
        verify(outboxRepository).flush();
    }

    @Test
    void noOpMutationDoesNotWriteSideEffects() {
        PaymentId paymentId =
                new PaymentId(UUID.randomUUID());
        Payment payment = Mockito.mock(Payment.class);

        when(paymentRepository.findById(paymentId))
                .thenReturn(Optional.of(payment));
        when(payment.businessVersion())
                .thenReturn(2L);
        when(payment.id()).thenReturn(paymentId);
        when(payment.publicPaymentReference())
                .thenReturn(Mockito.mock(
                        com.sixpay.payment.domain.model
                                .PublicPaymentReference.class
                ));
        when(payment.status())
                .thenReturn(
                        com.sixpay.payment.domain.model
                                .PaymentStatus.RECEIVED
                );

        PaymentWorkflowResult result =
                coordinator.mutate(
                        paymentId,
                        ignored -> {
                        }
                );

        assertThat(result.stateChanged()).isFalse();

        verify(paymentRepository, never())
                .save(Mockito.any());
        verify(auditAdapter, never())
                .appendAll(Mockito.anyList());
        verify(outboxRepository, never())
                .saveAll(Mockito.anyList());
    }
}
