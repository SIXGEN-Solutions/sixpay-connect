package com.sixpay.payment.application.service;

import com.sixpay.common.time.TimeProvider;
import com.sixpay.payment.application.port.out.PaymentAtomicPersistencePort;
import com.sixpay.payment.application.port.out.PaymentLookupPort;
import com.sixpay.payment.domain.event.PaymentDomainEvent;
import com.sixpay.payment.domain.model.Payment;
import com.sixpay.payment.domain.model.PaymentId;
import com.sixpay.payment.domain.model.PaymentStatus;
import com.sixpay.payment.domain.model.PublicPaymentReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentMutationCoordinatorTest {

    private static final Instant CURRENT_TIME =
            Instant.parse("2026-08-01T20:00:00Z");

    private PaymentLookupPort paymentLookupPort;
    private PaymentAtomicPersistencePort atomicPersistencePort;
    private PaymentMutationCoordinator coordinator;

    @BeforeEach
    void setUp() {
        paymentLookupPort = Mockito.mock(
                PaymentLookupPort.class
        );

        atomicPersistencePort = Mockito.mock(
                PaymentAtomicPersistencePort.class
        );

        TimeProvider timeProvider = () -> CURRENT_TIME;

        coordinator = new PaymentMutationCoordinator(
                paymentLookupPort,
                atomicPersistencePort,
                timeProvider
        );
    }

    @Test
    void persistsAggregateAuditAndOutboxAfterMutation() {
        PaymentId paymentId =
                new PaymentId(UUID.randomUUID());

        PublicPaymentReference publicReference =
                publicReference(paymentId);

        Payment payment = Mockito.mock(Payment.class);
        Payment persistedPayment = Mockito.mock(Payment.class);
        PaymentDomainEvent event =
                Mockito.mock(PaymentDomainEvent.class);

        Instant eventOccurredAt =
                Instant.parse("2026-08-01T19:59:59Z");

        when(paymentLookupPort.findById(paymentId))
                .thenReturn(Optional.of(payment));

        /*
         * Le premier appel représente la version avant mutation.
         * Le second appel représente la version après mutation.
         */
        when(payment.businessVersion())
                .thenReturn(1L, 2L);

        when(payment.domainEvents())
                .thenReturn(List.of(event));

        when(event.occurredAt())
                .thenReturn(eventOccurredAt);

        when(
                atomicPersistencePort.persist(
                        payment,
                        List.of(event),
                        CURRENT_TIME
                )
        ).thenReturn(persistedPayment);

        stubPaymentResult(
                persistedPayment,
                paymentId,
                publicReference,
                PaymentStatus.AUTHORIZATION_CHECKING,
                2L
        );

        PaymentWorkflowResult result =
                coordinator.mutate(
                        paymentId,
                        ignored -> {
                            // La mutation métier est simulée par
                            // l’évolution de businessVersion().
                        }
                );

        assertThat(result.paymentId())
                .isEqualTo(paymentId);

        assertThat(result.publicPaymentReference())
                .isEqualTo(publicReference);

        assertThat(result.status())
                .isEqualTo(
                        PaymentStatus.AUTHORIZATION_CHECKING
                );

        assertThat(result.businessVersion())
                .isEqualTo(2L);

        assertThat(result.stateChanged())
                .isTrue();

        verify(paymentLookupPort)
                .findById(paymentId);

        verify(atomicPersistencePort)
                .persist(
                        payment,
                        List.of(event),
                        CURRENT_TIME
                );
    }

    @Test
    void usesEventOccurrenceWhenCurrentTimePrecedesEvent() {
        PaymentId paymentId =
                new PaymentId(UUID.randomUUID());

        PublicPaymentReference publicReference =
                publicReference(paymentId);

        Payment payment = Mockito.mock(Payment.class);
        Payment persistedPayment = Mockito.mock(Payment.class);
        PaymentDomainEvent event =
                Mockito.mock(PaymentDomainEvent.class);

        Instant futureEventTime =
                CURRENT_TIME.plusSeconds(10);

        when(paymentLookupPort.findById(paymentId))
                .thenReturn(Optional.of(payment));

        when(payment.businessVersion())
                .thenReturn(1L, 2L);

        when(payment.domainEvents())
                .thenReturn(List.of(event));

        when(event.occurredAt())
                .thenReturn(futureEventTime);

        when(
                atomicPersistencePort.persist(
                        payment,
                        List.of(event),
                        futureEventTime
                )
        ).thenReturn(persistedPayment);

        stubPaymentResult(
                persistedPayment,
                paymentId,
                publicReference,
                PaymentStatus.AUTHORIZATION_CHECKING,
                2L
        );

        PaymentWorkflowResult result =
                coordinator.mutate(
                        paymentId,
                        ignored -> {
                        }
                );

        assertThat(result.stateChanged()).isTrue();

        verify(atomicPersistencePort)
                .persist(
                        payment,
                        List.of(event),
                        futureEventTime
                );
    }

    @Test
    void noOpMutationDoesNotWriteSideEffects() {
        PaymentId paymentId =
                new PaymentId(UUID.randomUUID());

        PublicPaymentReference publicReference =
                publicReference(paymentId);

        Payment payment = Mockito.mock(Payment.class);

        when(paymentLookupPort.findById(paymentId))
                .thenReturn(Optional.of(payment));

        /*
         * Même valeur avant et après la mutation :
         * le domaine n’a produit aucun changement.
         */
        when(payment.businessVersion())
                .thenReturn(2L);

        stubPaymentResult(
                payment,
                paymentId,
                publicReference,
                PaymentStatus.RECEIVED,
                2L
        );

        PaymentWorkflowResult result =
                coordinator.mutate(
                        paymentId,
                        ignored -> {
                        }
                );

        assertThat(result.paymentId())
                .isEqualTo(paymentId);

        assertThat(result.status())
                .isEqualTo(PaymentStatus.RECEIVED);

        assertThat(result.businessVersion())
                .isEqualTo(2L);

        assertThat(result.stateChanged())
                .isFalse();

        verify(paymentLookupPort)
                .findById(paymentId);

        verify(
                atomicPersistencePort,
                never()
        ).persist(
                any(Payment.class),
                any(),
                any(Instant.class)
        );

        verify(
                payment,
                never()
        ).domainEvents();
    }

    @Test
    void persistsNewPaymentWithItsEvents() {
        PaymentId paymentId =
                new PaymentId(UUID.randomUUID());

        PublicPaymentReference publicReference =
                publicReference(paymentId);

        Payment payment = Mockito.mock(Payment.class);
        Payment persistedPayment = Mockito.mock(Payment.class);
        PaymentDomainEvent event =
                Mockito.mock(PaymentDomainEvent.class);

        when(payment.domainEvents())
                .thenReturn(List.of(event));

        when(event.occurredAt())
                .thenReturn(
                        Instant.parse(
                                "2026-08-01T19:59:59Z"
                        )
                );

        when(
                atomicPersistencePort.persist(
                        payment,
                        List.of(event),
                        CURRENT_TIME
                )
        ).thenReturn(persistedPayment);

        stubPaymentResult(
                persistedPayment,
                paymentId,
                publicReference,
                PaymentStatus.RECEIVED,
                1L
        );

        PaymentWorkflowResult result =
                coordinator.persistNew(payment);

        assertThat(result.paymentId())
                .isEqualTo(paymentId);

        assertThat(result.status())
                .isEqualTo(PaymentStatus.RECEIVED);

        assertThat(result.businessVersion())
                .isEqualTo(1L);

        assertThat(result.stateChanged())
                .isTrue();

        verify(atomicPersistencePort)
                .persist(
                        payment,
                        List.of(event),
                        CURRENT_TIME
                );

        verify(
                paymentLookupPort,
                never()
        ).findById(any(PaymentId.class));
    }

    @Test
    void rejectsChangedPaymentWithoutDomainEvents() {
        PaymentId paymentId =
                new PaymentId(UUID.randomUUID());

        Payment payment = Mockito.mock(Payment.class);

        when(paymentLookupPort.findById(paymentId))
                .thenReturn(Optional.of(payment));

        when(payment.businessVersion())
                .thenReturn(1L, 2L);

        when(payment.domainEvents())
                .thenReturn(List.of());

        assertThatThrownBy(() ->
                coordinator.mutate(
                        paymentId,
                        ignored -> {
                        }
                )
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(
                        "must expose domain events"
                );

        verify(
                atomicPersistencePort,
                never()
        ).persist(
                any(Payment.class),
                any(),
                any(Instant.class)
        );
    }

    @Test
    void failsWhenPaymentDoesNotExist() {
        PaymentId paymentId =
                new PaymentId(UUID.randomUUID());

        when(paymentLookupPort.findById(paymentId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                coordinator.mutate(
                        paymentId,
                        ignored -> {
                        }
                )
        )
                .isInstanceOf(
                        PaymentNotFoundException.class
                )
                .hasMessageContaining(
                        paymentId.toString()
                );

        verify(
                atomicPersistencePort,
                never()
        ).persist(
                any(Payment.class),
                any(),
                any(Instant.class)
        );
    }

    private static void stubPaymentResult(
            Payment payment,
            PaymentId paymentId,
            PublicPaymentReference publicReference,
            PaymentStatus status,
            long businessVersion
    ) {
        when(payment.id()).thenReturn(paymentId);

        when(payment.publicPaymentReference())
                .thenReturn(publicReference);

        when(payment.status()).thenReturn(status);

        when(payment.businessVersion())
                .thenReturn(businessVersion);
    }

    private static PublicPaymentReference publicReference(
            PaymentId paymentId
    ) {
        String identifier = paymentId.value()
                .toString()
                .replace("-", "")
                .substring(0, 26)
                .toUpperCase();

        return PublicPaymentReference.of(
                "PAY-" + identifier
        );
    }
}