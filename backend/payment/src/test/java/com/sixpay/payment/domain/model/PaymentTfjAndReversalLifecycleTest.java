package com.sixpay.payment.domain.model;

import com.sixpay.payment.domain.exception.PaymentDomainException;

import com.sixpay.payment.domain.event.*;
import com.sixpay.payment.domain.model.evidence.ReversalSnapshot;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentTfjAndReversalLifecycleTest {

    @Test
    void uniquelyMatchedIntegratedTfjEstablishesTerminalFinality() {
        Payment payment =
                PaymentAggregateTestFixtures.postedPendingTfjPayment();

        payment.recordMatchedEndOfDayConfirmation(
                PaymentAggregateTestFixtures.tfjIntegrated("8"),
                PaymentAggregateTestFixtures.tfjProof(),
                null,
                PaymentAggregateTestFixtures.T0.plusSeconds(12),
                PaymentAggregateTestFixtures.profiles()
        );

        assertEquals(
                PaymentStatus.TREASURY_INTEGRATED,
                payment.status()
        );
        assertEquals(10L, payment.businessVersion());
        assertTrue(payment.toState().finalizedAt().isPresent());

        List<PaymentDomainEvent> events =
                payment.domainEvents().subList(
                        payment.domainEvents().size() - 3,
                        payment.domainEvents().size()
                );

        assertTrue(
                events.get(0)
                        instanceof PaymentEndOfDayConfirmationRecorded
        );
        assertTrue(
                events.get(1)
                        instanceof TreasuryIntegrationConfirmed
        );
        assertTrue(
                events.get(2)
                        instanceof PaymentFinalResultAvailable
        );
    }

    @Test
    void postingReversalRequirementCanBeAuthorizedAndConfirmed() {
        Payment payment =
                PaymentAggregateTestFixtures.postingPendingPayment();

        payment.recordPostingOutcome(
                PaymentAggregateTestFixtures
                        .reversalRequiredPosting("7"),
                PaymentAggregateTestFixtures
                        .reversalRequiredFailure(),
                PaymentAggregateTestFixtures.T0.plusSeconds(7),
                PaymentAggregateTestFixtures.profiles()
        );

        assertEquals(
                PaymentStatus.REVERSAL_REQUIRED,
                payment.status()
        );

        payment.authorizeReversal(
                PaymentAggregateTestFixtures.reversalInstruction(),
                PaymentAggregateTestFixtures.reversalAuthorization(),
                PaymentAggregateTestFixtures.T0.plusSeconds(13),
                PaymentAggregateTestFixtures.profiles()
        );

        assertEquals(
                PaymentStatus.REVERSAL_PENDING,
                payment.status()
        );

        List<PaymentDomainEvent> authorization =
                payment.domainEvents().subList(
                        payment.domainEvents().size() - 2,
                        payment.domainEvents().size()
                );
        assertTrue(
                authorization.get(0)
                        instanceof PaymentReversalAuthorized
        );
        assertTrue(
                authorization.get(1)
                        instanceof PaymentReversalRequested
        );

        ReversalSnapshot reversed =
                PaymentAggregateTestFixtures.reversedSnapshot("8");

        payment.recordReversalOutcome(
                reversed,
                null,
                PaymentAggregateTestFixtures.T0.plusSeconds(16),
                PaymentAggregateTestFixtures.profiles()
        );

        assertEquals(PaymentStatus.REVERSED, payment.status());
        assertTrue(payment.toState().finalizedAt().isPresent());
        assertEquals(
                "POSTING-001",
                payment.toState()
                        .bankPostingReference()
                        .orElseThrow()
                        .principalPostingReference()
        );

        List<PaymentDomainEvent> result =
                payment.domainEvents().subList(
                        payment.domainEvents().size() - 3,
                        payment.domainEvents().size()
                );
        assertTrue(
                result.get(0)
                        instanceof PaymentReversalOutcomeRecorded
        );
        assertTrue(result.get(1) instanceof PaymentReversed);
        assertTrue(
                result.get(2)
                        instanceof PaymentReversalResultAvailable
        );
    }


    @Test
    void unknownReversalOutcomeRequiresAuthoritativeResolution() {
        Payment payment =
                PaymentAggregateTestFixtures.postingPendingPayment();
        payment.recordPostingOutcome(
                PaymentAggregateTestFixtures
                        .reversalRequiredPosting("7"),
                PaymentAggregateTestFixtures
                        .reversalRequiredFailure(),
                PaymentAggregateTestFixtures.T0.plusSeconds(7),
                PaymentAggregateTestFixtures.profiles()
        );
        payment.authorizeReversal(
                PaymentAggregateTestFixtures.reversalInstruction(),
                PaymentAggregateTestFixtures.reversalAuthorization(),
                PaymentAggregateTestFixtures.T0.plusSeconds(13),
                PaymentAggregateTestFixtures.profiles()
        );

        payment.recordReversalOutcome(
                PaymentAggregateTestFixtures
                        .unknownReversalSnapshot("8"),
                PaymentAggregateTestFixtures.unknownFailure(
                        "REVERSAL_OUTCOME_UNKNOWN",
                        FailureStage.REVERSAL,
                        PaymentAggregateTestFixtures.T0.plusSeconds(15)
                ),
                PaymentAggregateTestFixtures.T0.plusSeconds(15),
                PaymentAggregateTestFixtures.profiles()
        );

        assertEquals(
                PaymentStatus.REVERSAL_OUTCOME_UNKNOWN,
                payment.status()
        );

        List<PaymentDomainEvent> unknownEvents =
                payment.domainEvents().subList(
                        payment.domainEvents().size() - 2,
                        payment.domainEvents().size()
                );
        assertTrue(
                unknownEvents.get(0)
                        instanceof PaymentReversalOutcomeRecorded
        );
        assertTrue(
                unknownEvents.get(1)
                        instanceof PaymentReversalOutcomeLookupRequested
        );

        payment.resolveReversalOutcome(
                PaymentAggregateTestFixtures
                        .resolvedReversalSnapshot("9"),
                null,
                PaymentAggregateTestFixtures.T0.plusSeconds(16),
                PaymentAggregateTestFixtures.profiles()
        );

        assertEquals(PaymentStatus.REVERSED, payment.status());

        List<PaymentDomainEvent> resolvedEvents =
                payment.domainEvents().subList(
                        payment.domainEvents().size() - 3,
                        payment.domainEvents().size()
                );
        assertTrue(
                resolvedEvents.get(0)
                        instanceof PaymentReversalOutcomeResolved
        );
        assertTrue(
                resolvedEvents.get(1) instanceof PaymentReversed
        );
        assertTrue(
                resolvedEvents.get(2)
                        instanceof PaymentReversalResultAvailable
        );
    }

    @Test
    void terminalPaymentRejectsNewStateChangingOperations() {
        Payment payment =
                PaymentAggregateTestFixtures.postedPendingTfjPayment();
        payment.recordMatchedEndOfDayConfirmation(
                PaymentAggregateTestFixtures.tfjIntegrated("8"),
                PaymentAggregateTestFixtures.tfjProof(),
                null,
                PaymentAggregateTestFixtures.T0.plusSeconds(12),
                PaymentAggregateTestFixtures.profiles()
        );

        PaymentState before = payment.toState();
        int eventCount = payment.domainEvents().size();

        assertThrows(
                PaymentDomainException.class,
                () -> payment.startAuthorizationChecking(
                        PaymentAggregateTestFixtures.T0
                                .plusSeconds(13)
                )
        );

        assertEquals(before, payment.toState());
        assertEquals(eventCount, payment.domainEvents().size());
    }

    @Test
    void identicalTerminalTfjReplayIsNoOp() {
        Payment payment =
                PaymentAggregateTestFixtures.postedPendingTfjPayment();
        payment.recordMatchedEndOfDayConfirmation(
                PaymentAggregateTestFixtures.tfjIntegrated("8"),
                PaymentAggregateTestFixtures.tfjProof(),
                null,
                PaymentAggregateTestFixtures.T0.plusSeconds(12),
                PaymentAggregateTestFixtures.profiles()
        );

        long version = payment.businessVersion();
        int eventCount = payment.domainEvents().size();

        payment.recordMatchedEndOfDayConfirmation(
                PaymentAggregateTestFixtures.tfjIntegrated("8"),
                PaymentAggregateTestFixtures.tfjProof(),
                null,
                PaymentAggregateTestFixtures.T0.plusSeconds(13),
                PaymentAggregateTestFixtures.profiles()
        );

        assertEquals(version, payment.businessVersion());
        assertEquals(eventCount, payment.domainEvents().size());
    }
}
