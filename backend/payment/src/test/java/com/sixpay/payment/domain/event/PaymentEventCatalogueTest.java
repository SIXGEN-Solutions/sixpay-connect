package com.sixpay.payment.domain.event;

import com.sixpay.payment.domain.model.Payment;
import com.sixpay.payment.domain.model.PaymentAggregateTestFixtures;
import com.sixpay.sharedkernel.domain.event.DomainEvent;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentEventCatalogueTest {

    private static final List<Class<? extends PaymentDomainEvent>>
            EVENT_TYPES = List.of(
                PaymentReceived.class,
                PaymentAuthorizationCheckingStarted.class,
                PaymentAuthorizationDecisionRecorded.class,
                PaymentBankingVerificationRequested.class,
                PaymentRejected.class,
                PaymentImmediateResultAvailable.class,
                PaymentBankingVerificationRecorded.class,
                PaymentFundsControlRequested.class,
                PaymentProcessingDeferred.class,
                PaymentFundsControlRecorded.class,
                PaymentTreasuryAccountResolutionRequested.class,
                PaymentTreasuryAccountResolutionRecorded.class,
                PaymentApprovedForPosting.class,
                PaymentPostingAuthorized.class,
                PaymentPostingRequested.class,
                PaymentPostingOutcomeRecorded.class,
                PaymentEndOfDayTrackingRequested.class,
                PaymentDebitConfirmed.class,
                PaymentPostingOutcomeLookupRequested.class,
                PaymentReversalRequired.class,
                PaymentPostingOutcomeResolved.class,
                PaymentEndOfDayConfirmationRecorded.class,
                TreasuryIntegrationConfirmed.class,
                PaymentFinalResultAvailable.class,
                PaymentTreasuryReconciliationRequired.class,
                PaymentReversalAuthorized.class,
                PaymentReversalRequested.class,
                PaymentReversalOutcomeRecorded.class,
                PaymentReversalResultAvailable.class,
                PaymentReversalOutcomeLookupRequested.class,
                PaymentReversalOutcomeResolved.class,
                PaymentFailedWithoutFinancialEffect.class,
                PaymentReversed.class
            );

    @Test
    void catalogueContainsExactlyThirtyThreeStableEventTypes() {
        assertEquals(33, EVENT_TYPES.size());
        assertEquals(
                33,
                EVENT_TYPES.stream()
                        .map(Class::getSimpleName)
                        .collect(Collectors.toSet())
                        .size()
        );

        EVENT_TYPES.forEach(type -> {
            assertTrue(PaymentDomainEvent.class.isAssignableFrom(type));
            assertTrue(DomainEvent.class.isAssignableFrom(type));
            assertTrue(type.isRecord());
        });
    }

    @Test
    void emittedEventsUseSimpleNameUuidV4AndSafeMetadata() {
        Payment payment =
                PaymentAggregateTestFixtures.newPayment();
        PaymentDomainEvent event =
                payment.domainEvents().getFirst();

        assertEquals(
                event.getClass().getSimpleName(),
                event.eventType()
        );
        assertEquals(4, event.eventId().version());
        assertFalse(event.eventId().equals(
                new java.util.UUID(0L, 0L)
        ));
        assertEquals(1L, event.aggregateVersion());
        assertEquals(1, event.eventSequence());
    }

    @Test
    void explicitEventsNeverContainWholeAggregateOrSnapshotFields() {
        Set<Class<?>> forbidden = Set.of(
                com.sixpay.payment.domain.model.Payment.class,
                com.sixpay.payment.domain.model.PaymentState.class,
                com.sixpay.payment.domain.model.evidence
                        .AuthorizationEvidenceSnapshot.class,
                com.sixpay.payment.domain.model.evidence
                        .BankingVerificationSnapshot.class,
                com.sixpay.payment.domain.model.evidence
                        .FundsControlSnapshot.class,
                com.sixpay.payment.domain.model.evidence
                        .TreasuryAccountResolutionSnapshot.class,
                com.sixpay.payment.domain.model.evidence
                        .PostingOutcomeSnapshot.class,
                com.sixpay.payment.domain.model.evidence
                        .EndOfDayConfirmationSnapshot.class,
                com.sixpay.payment.domain.model.evidence
                        .ReversalSnapshot.class
        );

        EVENT_TYPES.forEach(type ->
                java.util.Arrays.stream(type.getRecordComponents())
                        .forEach(component ->
                                assertFalse(
                                        forbidden.contains(
                                                component.getType()
                                        ),
                                        () -> type.getSimpleName()
                                                + " exposes "
                                                + component.getType()
                                )
                        )
        );
    }
}
