package com.sixpay.payment.application.service;

import com.sixpay.common.time.TimeProvider;
import com.sixpay.payment.application.exception.PaymentConcurrentMutationException;
import com.sixpay.payment.application.port.output.PaymentLookupPort;
import com.sixpay.payment.application.port.output.PaymentAtomicPersistencePort;
import com.sixpay.payment.domain.event.PaymentDomainEvent;
import com.sixpay.payment.domain.model.Payment;
import com.sixpay.payment.domain.model.PaymentId;
import com.sixpay.payment.domain.model.PaymentStatus;
import com.sixpay.payment.domain.model.PublicPaymentReference;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** A failed optimistic write must not transparently replay an arbitrary mutation. */
class PaymentMutationConcurrencySafetyTest {
    @Test
    void coordinatorDoesNotBlindlyRetryAfterConcurrentWrite() {
        PaymentLookupPort lookup = mock(PaymentLookupPort.class);
        PaymentAtomicPersistencePort persistence = mock(PaymentAtomicPersistencePort.class);
        Payment payment = mock(Payment.class);
        PaymentDomainEvent event = mock(PaymentDomainEvent.class);
        PaymentId id = new PaymentId(UUID.fromString(
                "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa"));
        Instant now = Instant.parse("2026-11-14T12:00:00Z");
        when(lookup.findById(id)).thenReturn(Optional.of(payment));
        when(payment.businessVersion()).thenReturn(1L, 2L);
        when(payment.domainEvents()).thenReturn(List.of(event));
        when(event.occurredAt()).thenReturn(now);
        when(persistence.persist(eq(payment), any(), any())).thenThrow(
                new PaymentConcurrentMutationException("lost write"));

        PaymentMutationCoordinator coordinator = new PaymentMutationCoordinator(
                lookup, persistence, (TimeProvider) () -> now);
        assertThrows(PaymentConcurrentMutationException.class,
                () -> coordinator.mutate(id, aggregate -> { }));

        verify(lookup, times(1)).findById(id);
        verify(persistence, times(1)).persist(eq(payment), any(), any());
    }
}
