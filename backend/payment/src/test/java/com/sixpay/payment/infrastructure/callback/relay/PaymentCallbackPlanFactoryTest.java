package com.sixpay.payment.infrastructure.callback.relay;

import com.sixpay.payment.application.port.output.callback.PaymentStatusCallbackMessage;
import com.sixpay.payment.domain.event.PaymentEndOfDayConfirmationRecorded;
import com.sixpay.payment.domain.event.PaymentEventOutcomeRecorded;
import com.sixpay.payment.domain.model.PaymentStatus;
import com.sixpay.payment.infrastructure.audit.PaymentAuditEntry;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentCallbackPlanFactoryTest {

    @Test
    void cutCreditedIsBoundToDurableCompletedT0OutcomeEvent() throws Exception {
        var method = PaymentCallbackPlanFactory.class
                .getDeclaredMethod("callbackType", PaymentAuditEntry.class);
        method.setAccessible(true);

        PaymentAuditEntry completedT0 = audit(
                PaymentEventOutcomeRecorded.class.getSimpleName(),
                PaymentStatus.POSTED_PENDING_TFJ
        );

        assertThat(method.invoke(null, completedT0))
                .isEqualTo(PaymentStatusCallbackMessage.CUT_CREDITED);
    }

    @Test
    void postedPendingTfjStatusFromAnotherEventDoesNotEmitCutCredited() throws Exception {
        var method = PaymentCallbackPlanFactory.class
                .getDeclaredMethod("callbackType", PaymentAuditEntry.class);
        method.setAccessible(true);

        PaymentAuditEntry unrelated = audit(
                "PaymentFinalResultAvailable",
                PaymentStatus.POSTED_PENDING_TFJ
        );

        assertThat(method.invoke(null, unrelated)).isNull();
    }

    @Test
    void nonT0StatusesDoNotEmitCutCredited() throws Exception {
        var method = PaymentCallbackPlanFactory.class
                .getDeclaredMethod("callbackType", PaymentAuditEntry.class);
        method.setAccessible(true);

        PaymentAuditEntry unknown = audit(
                PaymentEventOutcomeRecorded.class.getSimpleName(),
                PaymentStatus.POSTING_OUTCOME_UNKNOWN
        );

        assertThat(method.invoke(null, unknown)).isNull();
    }

    @Test
    void treasuryIntegratedIsBoundToDurableTfjConfirmationEvent() throws Exception {
        var method = PaymentCallbackPlanFactory.class
                .getDeclaredMethod("callbackType", PaymentAuditEntry.class);
        method.setAccessible(true);

        PaymentAuditEntry integrated = audit(
                PaymentEndOfDayConfirmationRecorded.class.getSimpleName(),
                PaymentStatus.TREASURY_INTEGRATED
        );

        assertThat(method.invoke(null, integrated))
                .isEqualTo(PaymentStatusCallbackMessage.TREASURY_INTEGRATED);
    }

    @Test
    void treasuryIntegratedStatusFromAnotherEventDoesNotEmitCallback() throws Exception {
        var method = PaymentCallbackPlanFactory.class
                .getDeclaredMethod("callbackType", PaymentAuditEntry.class);
        method.setAccessible(true);

        PaymentAuditEntry unrelated = audit(
                "PaymentFinalResultAvailable",
                PaymentStatus.TREASURY_INTEGRATED
        );

        assertThat(method.invoke(null, unrelated)).isNull();
    }

    @Test
    void cutCreditedEligibilityDoesNotRequireLastSequenceOfVersion() throws Exception {
        var method = PaymentCallbackPlanFactory.class
                .getDeclaredMethod("callbackType", PaymentAuditEntry.class);
        method.setAccessible(true);
        // Sequence 1 is followed by ImmediateResultAvailable and TFJ tracking.
        PaymentAuditEntry first = audit(
                PaymentEventOutcomeRecorded.class.getSimpleName(),
                PaymentStatus.POSTED_PENDING_TFJ);
        assertThat(first.eventSequence()).isEqualTo(1);
        assertThat(method.invoke(null, first))
                .isEqualTo(PaymentStatusCallbackMessage.CUT_CREDITED);
    }

    private static PaymentAuditEntry audit(
            String eventType,
            PaymentStatus status
    ) {
        return new PaymentAuditEntry(
                UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"),
                UUID.fromString("11111111-2222-3333-4444-555555555555"),
                "PAY-1234567890ABCDEFGHJKMNPQRS",
                eventType,
                status,
                10L,
                1,
                "11111111-1111-1111-1111-111111111111",
                null,
                Instant.parse("2026-09-18T10:00:00Z")
        );
    }
    // BEGIN SIXPAY_R6_T0_VALIDATION_TESTS
    @Test
    void r6RejectedT0EventMapsToPaymentRejected() throws Exception {
        var method = PaymentCallbackPlanFactory.class
                .getDeclaredMethod("callbackType", PaymentAuditEntry.class);
        method.setAccessible(true);
        PaymentAuditEntry rejected = audit(
                PaymentEventOutcomeRecorded.class.getSimpleName(),
                PaymentStatus.REJECTED);
        assertThat(method.invoke(null, rejected))
                .isEqualTo(PaymentStatusCallbackMessage.PAYMENT_REJECTED);
    }

    @Test
    void unrelatedRejectedEventDoesNotEmitT0Callback() throws Exception {
        var method = PaymentCallbackPlanFactory.class
                .getDeclaredMethod("callbackType", PaymentAuditEntry.class);
        method.setAccessible(true);
        assertThat(method.invoke(null, audit("PaymentRejected", PaymentStatus.REJECTED)))
                .isNull();
    }

    @Test
    void rejectedT0BuildsImmutablePaymentRejectedDelivery() {
        var repository = org.mockito.Mockito.mock(
                com.sixpay.payment.domain.repository.PaymentRepository.class);
        var auditStore = org.mockito.Mockito.mock(
                com.sixpay.payment.infrastructure.audit.PaymentAuditAdapter.class);
        var payment = org.mockito.Mockito.mock(
                com.sixpay.payment.domain.model.Payment.class);
        var state = org.mockito.Mockito.mock(
                com.sixpay.payment.domain.model.PaymentState.class);
        var initiation = org.mockito.Mockito.mock(
                com.sixpay.payment.domain.model.PaymentInitiationContext.class);
        var evidence = org.mockito.Mockito.mock(
                com.sixpay.payment.domain.model.evidence.PaymentEventOutcomeSnapshot.class);
        var failure = org.mockito.Mockito.mock(
                com.sixpay.payment.domain.model.PaymentFailure.class);

        UUID paymentId = UUID.fromString("11111111-2222-3333-4444-555555555555");
        UUID eventId = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
        PaymentAuditEntry entry = audit(
                PaymentEventOutcomeRecorded.class.getSimpleName(), PaymentStatus.REJECTED);
        String payload = """
                {"metadata":{"eventId":"aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee",
                "occurredAt":"2026-09-18T10:00:00Z", "paymentStatus":"REJECTED",
                "aggregateVersion":10, "eventSequence":1},
                "outcome":"REJECTED", "reasonCode":{"value":"BANK_REJECTED"}}
                """;
        var event = new ClaimedPaymentOutboxEvent(eventId, paymentId,
                "payment.domain-event", entry.correlationId(), entry.occurredAt(), 1, payload);
        org.mockito.Mockito.when(auditStore.findByPaymentId(paymentId))
                .thenReturn(java.util.List.of(entry));
        org.mockito.Mockito.when(repository.findById(
                new com.sixpay.payment.domain.model.PaymentId(paymentId)))
                .thenReturn(java.util.Optional.of(payment));
        org.mockito.Mockito.when(payment.toState()).thenReturn(state);
        org.mockito.Mockito.when(state.initiationContext())
                .thenReturn(java.util.Optional.of(initiation));
        org.mockito.Mockito.when(initiation.callbackEndpoint())
                .thenReturn(com.sixpay.payment.domain.model.CallbackEndpoint.of(
                        "https://partner.example/callback"));
        org.mockito.Mockito.when(state.paymentEventOutcomeEvidence())
                .thenReturn(java.util.Optional.of(evidence));
        org.mockito.Mockito.when(evidence.outcome())
                .thenReturn(com.sixpay.payment.domain.model.evidence.PaymentEventOutcome.REJECTED);
        org.mockito.Mockito.when(state.failure())
                .thenReturn(java.util.Optional.of(failure));
        org.mockito.Mockito.when(failure.failureCode())
                .thenReturn(com.sixpay.payment.domain.model.FailureCode.of("BANK_REJECTED"));
        org.mockito.Mockito.when(state.publicPaymentReference())
                .thenReturn(com.sixpay.payment.domain.model.PublicPaymentReference.of(
                        "PAY-01ARZ3NDEKTSV4RRFFQ69G5FAV"));
        org.mockito.Mockito.when(state.externalPaymentReference())
                .thenReturn(com.sixpay.payment.domain.model.ExternalPaymentReference.of(
                        "AVI-2026-00001"));
        org.mockito.Mockito.when(state.financialInstitutionCode())
                .thenReturn(com.sixpay.payment.domain.model.FinancialInstitutionCode.of("LRB"));

        var factory = new PaymentCallbackPlanFactory(repository, auditStore,
                new tools.jackson.databind.ObjectMapper());
        PaymentCallbackPlan plan = factory.create(event);
        assertThat(plan.deliver()).isTrue();
        var delivery = plan.delivery();
        assertThat(delivery.message().eventId()).isEqualTo(eventId);
        assertThat(delivery.message().eventType())
                .isEqualTo(PaymentStatusCallbackMessage.PAYMENT_REJECTED);
        assertThat(delivery.message().data())
                .isEqualTo(new PaymentStatusCallbackMessage.PaymentRejectedData(
                        "REJECTED", "BANK_REJECTED"));
        assertThat(factory.create(event).delivery().message()).isEqualTo(delivery.message());
        assertThat(factory.create(event).delivery().deliveryId())
                .isNotEqualTo(delivery.deliveryId());

        var coordinator = org.mockito.Mockito.mock(PaymentCallbackOutboxCoordinator.class);
        var transport = org.mockito.Mockito.mock(
                com.sixpay.payment.application.port.output.callback
                        .PaymentStatusCallbackTransportPort.class);
        org.mockito.Mockito.when(coordinator.claim())
                .thenReturn(java.util.List.of(event));
        new PaymentCallbackOutboxRelay(coordinator, factory, transport)
                .publishAvailableCallbacks();
        org.mockito.Mockito.verify(transport).send(
                org.mockito.ArgumentMatchers.argThat(sent ->
                        sent.message().eventType().equals(
                                PaymentStatusCallbackMessage.PAYMENT_REJECTED)
                        && sent.message().eventId().equals(eventId)));
        org.mockito.Mockito.verify(coordinator).markPublished(eventId, 1);
    }
    // END SIXPAY_R6_T0_VALIDATION_TESTS

}
