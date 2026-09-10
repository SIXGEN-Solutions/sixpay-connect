package com.sixpay.payment.domain.model;

import com.sixpay.payment.domain.exception.PaymentDomainException;

import com.sixpay.payment.domain.event.*;
import com.sixpay.payment.domain.model.evidence.AuthorizationEvidenceSnapshot;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentPreFinancialLifecycleTest {

    @Test
    void approvedSixpayAuthorizationMovesToFundsControlPending() {
        Payment payment =
                PaymentAggregateTestFixtures.authorizationCheckingPayment();
        Instant decisionAt =
                PaymentAggregateTestFixtures.T0.plusSeconds(4);

        int previousEventCount = payment.domainEvents().size();

        payment.approveSixpayAuthorization(decisionAt);

        assertEquals(
                PaymentStatus.FUNDS_CONTROL_PENDING,
                payment.status()
        );
        assertEquals(
                previousEventCount + 1,
                payment.domainEvents().size()
        );
        assertTrue(
                payment.domainEvents().get(
                        payment.domainEvents().size() - 1
                ) instanceof PaymentFundsControlRequested
        );
    }

    @Test
    void replayedSixpayAuthorizationApprovalIsStrictNoOp() {
        Payment payment =
                PaymentAggregateTestFixtures.authorizationCheckingPayment();
        Instant decisionAt =
                PaymentAggregateTestFixtures.T0.plusSeconds(4);

        payment.approveSixpayAuthorization(decisionAt);

        long version = payment.businessVersion();
        int eventCount = payment.domainEvents().size();

        payment.approveSixpayAuthorization(
                PaymentAggregateTestFixtures.T0.plusSeconds(5)
        );

        assertEquals(version, payment.businessVersion());
        assertEquals(eventCount, payment.domainEvents().size());
    }

    @Test
    void favorableEvidenceProgressesToApprovedForPosting() {
        Payment payment = PaymentAggregateTestFixtures.approvedPayment();

        assertEquals(
                PaymentStatus.APPROVED_FOR_POSTING,
                payment.status()
        );
        assertEquals(7L, payment.businessVersion());
        assertEquals(12, payment.domainEvents().size());

        List<PaymentDomainEvent> lastMutation =
                payment.domainEvents().subList(10, 12);

        assertTrue(
                lastMutation.get(0)
                        instanceof PaymentTreasuryAccountResolutionRecorded
        );
        assertTrue(
                lastMutation.get(1)
                        instanceof PaymentApprovedForPosting
        );
        assertEquals(7L, lastMutation.get(0).aggregateVersion());
        assertEquals(7L, lastMutation.get(1).aggregateVersion());
        assertEquals(1, lastMutation.get(0).eventSequence());
        assertEquals(2, lastMutation.get(1).eventSequence());
    }

    @Test
    void identicalAuthorizationReplayIsStrictNoOp() {
        Payment payment =
                PaymentAggregateTestFixtures.authorizationCheckingPayment();
        AuthorizationEvidenceSnapshot evidence =
                PaymentAggregateTestFixtures.authorizationApproved("3");

        payment.recordAuthorizationDecision(
                evidence,
                null,
                PaymentAggregateTestFixtures.T0.plusSeconds(4),
                PaymentAggregateTestFixtures.profiles()
        );

        long version = payment.businessVersion();
        int eventCount = payment.domainEvents().size();

        payment.recordAuthorizationDecision(
                PaymentAggregateTestFixtures
                        .authorizationApproved("3"),
                null,
                PaymentAggregateTestFixtures.T0.plusSeconds(5),
                PaymentAggregateTestFixtures.profiles()
        );

        assertEquals(version, payment.businessVersion());
        assertEquals(eventCount, payment.domainEvents().size());
    }

    @Test
    void conflictingAuthorizationLeavesAggregateUntouched() {
        Payment payment =
                PaymentAggregateTestFixtures.authorizationCheckingPayment();
        payment.recordAuthorizationDecision(
                PaymentAggregateTestFixtures
                        .authorizationApproved("3"),
                null,
                PaymentAggregateTestFixtures.T0.plusSeconds(4),
                PaymentAggregateTestFixtures.profiles()
        );

        PaymentState before = payment.toState();
        int eventCount = payment.domainEvents().size();

        assertThrows(
                PaymentDomainException.class,
                () -> payment.recordAuthorizationDecision(
                        PaymentAggregateTestFixtures
                                .authorizationApproved("9"),
                        null,
                        PaymentAggregateTestFixtures.T0
                                .plusSeconds(5),
                        PaymentAggregateTestFixtures.profiles()
                )
        );

        assertEquals(before, payment.toState());
        assertEquals(eventCount, payment.domainEvents().size());
    }

    @Test
    void conclusiveRejectionIsTerminalAndProducesResultIntent() {
        Payment payment = PaymentAggregateTestFixtures.newPayment();
        PaymentFailure rejection =
                PaymentAggregateTestFixtures.businessFailure(
                        "INVALID_REQUEST",
                        FailureStage.INTAKE,
                        PaymentAggregateTestFixtures.T0.plusSeconds(1)
                );

        payment.reject(
                rejection,
                PaymentAggregateTestFixtures.T0.plusSeconds(1),
                PaymentAggregateTestFixtures.profiles()
        );

        assertEquals(PaymentStatus.REJECTED, payment.status());
        assertEquals(2L, payment.businessVersion());
        assertTrue(payment.toState().finalizedAt().isPresent());

        List<PaymentDomainEvent> events =
                payment.domainEvents().subList(1, 3);
        assertTrue(events.get(0) instanceof PaymentRejected);
        assertTrue(
                events.get(1)
                        instanceof PaymentImmediateResultAvailable
        );
    }

    @Test
    void recoverableFailureKeepsConcreteStateAndIncrementsOnce() {
        Payment payment =
                PaymentAggregateTestFixtures.authorizationCheckingPayment();

        PaymentFailure failure =
                PaymentAggregateTestFixtures.technicalFailure(
                        "JWKS_TEMPORARILY_UNAVAILABLE",
                        FailureStage.AUTHORIZATION,
                        PaymentAggregateTestFixtures.T0.plusSeconds(4)
                );

        payment.recordRecoverableFailure(
                failure,
                PaymentAggregateTestFixtures.T0.plusSeconds(4),
                PaymentAggregateTestFixtures.profiles()
        );

        assertEquals(
                PaymentStatus.AUTHORIZATION_CHECKING,
                payment.status()
        );
        assertEquals(5L, payment.businessVersion());

        List<PaymentDomainEvent> events =
                payment.domainEvents().subList(
                        payment.domainEvents().size() - 2,
                        payment.domainEvents().size()
                );
        assertTrue(events.get(0) instanceof PaymentProcessingDeferred);
        assertTrue(
                events.get(1)
                        instanceof PaymentImmediateResultAvailable
        );
    }

    @Test
    void invalidTransitionCreatesNoMutationOrEvent() {
        Payment payment = PaymentAggregateTestFixtures.newPayment();
        PaymentState before = payment.toState();
        int eventCount = payment.domainEvents().size();

        assertThrows(
                PaymentDomainException.class,
                () -> payment.recordFundsControl(
                        PaymentAggregateTestFixtures
                                .fundsVerified("5"),
                        null,
                        PaymentAggregateTestFixtures.T0
                                .plusSeconds(2),
                        PaymentAggregateTestFixtures.profiles()
                )
        );

        assertEquals(before, payment.toState());
        assertEquals(eventCount, payment.domainEvents().size());
    }
}
