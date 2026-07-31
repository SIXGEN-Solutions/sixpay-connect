package com.sixpay.payment.domain.model;

import com.sixpay.payment.domain.event.PaymentReceived;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentCreationAndReconstitutionTest {

    @Test
    void receiveCreatesVersionOneAndOneOrderedEvent() {
        Payment payment = PaymentAggregateTestFixtures.newPayment();

        assertEquals(PaymentStatus.RECEIVED, payment.status());
        assertEquals(1L, payment.businessVersion());
        assertEquals(1, payment.domainEvents().size());
        assertTrue(
                payment.domainEvents().getFirst()
                        instanceof PaymentReceived
        );

        PaymentReceived event = (PaymentReceived)
                payment.domainEvents().getFirst();

        assertEquals(payment.id(), event.paymentId());
        assertEquals(
                payment.publicPaymentReference(),
                event.paymentReference()
        );
        assertEquals(1L, event.aggregateVersion());
        assertEquals(1, event.eventSequence());
        assertEquals(PaymentStatus.RECEIVED, event.paymentStatus());
        assertEquals(
                PaymentAggregateTestFixtures.T0,
                event.occurredAt()
        );
        assertEquals("PaymentReceived", event.eventType());
    }

    @Test
    void reconstitutionRestoresStateWithoutEventOrMutation() {
        Payment original = PaymentAggregateTestFixtures.approvedPayment();
        PaymentState state = original.toState();

        Payment restored = Payment.reconstitute(state);

        assertEquals(state, restored.toState());
        assertEquals(state.status(), restored.status());
        assertEquals(
                state.businessVersion(),
                restored.businessVersion()
        );
        assertTrue(restored.domainEvents().isEmpty());
    }

    @Test
    void stateRejectsTerminalStatusWithoutFinalizedAt() {
        PaymentState valid =
                PaymentAggregateTestFixtures.newPayment().toState();

        assertThrows(
                IllegalArgumentException.class,
                () -> valid.toBuilder()
                        .status(PaymentStatus.REJECTED)
                        .failure(
                                PaymentAggregateTestFixtures
                                        .businessFailure(
                                                "REJECTED",
                                                FailureStage.INTAKE,
                                                PaymentAggregateTestFixtures.T0
                                        )
                        )
                        .businessVersion(2)
                        .updatedAt(
                                PaymentAggregateTestFixtures.T0
                                        .plusSeconds(1)
                        )
                        .finalizedAt(null)
                        .build()
        );
    }

    @Test
    void domainEventsViewIsImmutable() {
        Payment payment = PaymentAggregateTestFixtures.newPayment();

        assertThrows(
                UnsupportedOperationException.class,
                () -> payment.domainEvents().clear()
        );
        assertFalse(payment.toState().status().isTerminal());
    }
}
