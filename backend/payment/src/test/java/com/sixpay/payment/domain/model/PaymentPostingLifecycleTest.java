package com.sixpay.payment.domain.model;

import com.sixpay.payment.domain.event.*;
import com.sixpay.payment.domain.model.evidence.EvidenceObservationChannel;
import com.sixpay.payment.domain.model.evidence.PostingOutcomeSnapshot;
import com.sixpay.payment.domain.policy.PostingInstructionIdentity;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentPostingLifecycleTest {

    @Test
    void completedPostingProducesResultAndTfjTrackingInOrder() {
        Payment payment =
                PaymentAggregateTestFixtures.postingPendingPayment();

        payment.recordPostingOutcome(
                PaymentAggregateTestFixtures.completedPosting(
                        "7",
                        EvidenceObservationChannel.DIRECT_RESPONSE
                ),
                null,
                PaymentAggregateTestFixtures.T0.plusSeconds(7),
                PaymentAggregateTestFixtures.profiles()
        );

        assertEquals(
                PaymentStatus.POSTED_PENDING_TFJ,
                payment.status()
        );
        assertEquals(8L, payment.businessVersion());

        List<PaymentDomainEvent> events = payment.domainEvents();
        List<PaymentDomainEvent> mutation =
                events.subList(events.size() - 3, events.size());

        assertTrue(
                mutation.get(0)
                        instanceof PaymentPostingOutcomeRecorded
        );
        assertTrue(
                mutation.get(1)
                        instanceof PaymentImmediateResultAvailable
        );
        assertTrue(
                mutation.get(2)
                        instanceof PaymentEndOfDayTrackingRequested
        );

        for (int index = 0; index < mutation.size(); index++) {
            assertEquals(8L, mutation.get(index).aggregateVersion());
            assertEquals(index + 1, mutation.get(index).eventSequence());
            assertEquals(
                    PaymentStatus.POSTED_PENDING_TFJ,
                    mutation.get(index).paymentStatus()
            );
        }
    }

    @Test
    void unknownPostingCanOnlyBeResolvedByAuthoritativeEvidence() {
        Payment payment =
                PaymentAggregateTestFixtures.postingPendingPayment();

        payment.recordPostingOutcome(
                PaymentAggregateTestFixtures.unknownPosting("7"),
                PaymentAggregateTestFixtures.unknownFailure(
                        "POSTING_OUTCOME_UNKNOWN",
                        FailureStage.POSTING,
                        PaymentAggregateTestFixtures.T0.plusSeconds(7)
                ),
                PaymentAggregateTestFixtures.T0.plusSeconds(7),
                PaymentAggregateTestFixtures.profiles()
        );

        assertEquals(
                PaymentStatus.POSTING_OUTCOME_UNKNOWN,
                payment.status()
        );
        assertEquals(8L, payment.businessVersion());

        List<PaymentDomainEvent> firstMutation =
                payment.domainEvents().subList(
                        payment.domainEvents().size() - 3,
                        payment.domainEvents().size()
                );
        assertTrue(
                firstMutation.get(0)
                        instanceof PaymentPostingOutcomeRecorded
        );
        assertTrue(
                firstMutation.get(1)
                        instanceof PaymentPostingOutcomeLookupRequested
        );
        assertTrue(
                firstMutation.get(2)
                        instanceof PaymentImmediateResultAvailable
        );

        PostingOutcomeSnapshot authoritative =
                PaymentAggregateTestFixtures.completedPosting(
                        "8",
                        EvidenceObservationChannel
                                .BANK_REFERENCE_LOOKUP
                );

        payment.resolvePostingOutcome(
                authoritative,
                null,
                PaymentAggregateTestFixtures.T0.plusSeconds(8),
                PaymentAggregateTestFixtures.profiles()
        );

        assertEquals(
                PaymentStatus.POSTED_PENDING_TFJ,
                payment.status()
        );
        assertEquals(9L, payment.businessVersion());

        List<PaymentDomainEvent> resolution =
                payment.domainEvents().subList(
                        payment.domainEvents().size() - 3,
                        payment.domainEvents().size()
                );
        assertTrue(
                resolution.get(0)
                        instanceof PaymentPostingOutcomeResolved
        );
        assertTrue(
                resolution.get(1)
                        instanceof PaymentImmediateResultAvailable
        );
        assertTrue(
                resolution.get(2)
                        instanceof PaymentEndOfDayTrackingRequested
        );
    }

    @Test
    void directResponseCannotResolveUnknownPosting() {
        Payment payment =
                PaymentAggregateTestFixtures.postingPendingPayment();

        payment.recordPostingOutcome(
                PaymentAggregateTestFixtures.unknownPosting("7"),
                PaymentAggregateTestFixtures.unknownFailure(
                        "POSTING_OUTCOME_UNKNOWN",
                        FailureStage.POSTING,
                        PaymentAggregateTestFixtures.T0.plusSeconds(7)
                ),
                PaymentAggregateTestFixtures.T0.plusSeconds(7),
                PaymentAggregateTestFixtures.profiles()
        );

        PaymentState before = payment.toState();
        int eventCount = payment.domainEvents().size();

        assertThrows(
                PaymentDomainException.class,
                () -> payment.resolvePostingOutcome(
                        PaymentAggregateTestFixtures.completedPosting(
                                "8",
                                EvidenceObservationChannel
                                        .DIRECT_RESPONSE
                        ),
                        null,
                        PaymentAggregateTestFixtures.T0
                                .plusSeconds(8),
                        PaymentAggregateTestFixtures.profiles()
                )
        );

        assertEquals(before, payment.toState());
        assertEquals(eventCount, payment.domainEvents().size());
    }


    @Test
    void debitOnlyOutcomeRemainsNonFinalAndRequestsProcessingResult() {
        Payment payment =
                PaymentAggregateTestFixtures.postingPendingPayment();

        payment.recordPostingOutcome(
                PaymentAggregateTestFixtures
                        .debitConfirmedPosting("7"),
                null,
                PaymentAggregateTestFixtures.T0.plusSeconds(7),
                PaymentAggregateTestFixtures.profiles()
        );

        assertEquals(
                PaymentStatus.DEBIT_CONFIRMED,
                payment.status()
        );

        List<PaymentDomainEvent> events =
                payment.domainEvents().subList(
                        payment.domainEvents().size() - 3,
                        payment.domainEvents().size()
                );
        assertTrue(
                events.get(0)
                        instanceof PaymentPostingOutcomeRecorded
        );
        assertTrue(events.get(1) instanceof PaymentDebitConfirmed);
        assertTrue(
                events.get(2)
                        instanceof PaymentImmediateResultAvailable
        );
    }

    @Test
    void businessAndTechnicalNoEffectOutcomesRemainDistinct() {
        Payment business =
                PaymentAggregateTestFixtures.postingPendingPayment();
        PaymentFailure rejection =
                PaymentAggregateTestFixtures.businessFailure(
                        "POSTING_REJECTED",
                        FailureStage.POSTING,
                        PaymentAggregateTestFixtures.T0.plusSeconds(7)
                );

        business.recordPostingOutcome(
                PaymentAggregateTestFixtures
                        .rejectedPostingWithoutEffect(
                                "7",
                                "POSTING_REJECTED"
                        ),
                rejection,
                PaymentAggregateTestFixtures.T0.plusSeconds(7),
                PaymentAggregateTestFixtures.profiles()
        );

        assertEquals(PaymentStatus.REJECTED, business.status());

        Payment technical =
                PaymentAggregateTestFixtures.postingPendingPayment();
        PaymentFailure failure =
                PaymentAggregateTestFixtures.technicalFailure(
                        "POSTING_TECHNICAL_FAILURE",
                        FailureStage.POSTING,
                        PaymentAggregateTestFixtures.T0.plusSeconds(7)
                );

        technical.recordPostingOutcome(
                PaymentAggregateTestFixtures
                        .rejectedPostingWithoutEffect(
                                "8",
                                "POSTING_TECHNICAL_FAILURE"
                        ),
                failure,
                PaymentAggregateTestFixtures.T0.plusSeconds(7),
                PaymentAggregateTestFixtures.profiles()
        );

        assertEquals(PaymentStatus.FAILED, technical.status());
    }

    @Test
    void identicalPostingEvidenceReplayIsNoOp() {
        Payment payment =
                PaymentAggregateTestFixtures.postingPendingPayment();
        PostingOutcomeSnapshot evidence =
                PaymentAggregateTestFixtures.completedPosting(
                        "7",
                        EvidenceObservationChannel.DIRECT_RESPONSE
                );

        payment.recordPostingOutcome(
                evidence,
                null,
                PaymentAggregateTestFixtures.T0.plusSeconds(7),
                PaymentAggregateTestFixtures.profiles()
        );

        long version = payment.businessVersion();
        int eventCount = payment.domainEvents().size();

        payment.recordPostingOutcome(
                PaymentAggregateTestFixtures.completedPosting(
                        "7",
                        EvidenceObservationChannel.DIRECT_RESPONSE
                ),
                null,
                PaymentAggregateTestFixtures.T0.plusSeconds(8),
                PaymentAggregateTestFixtures.profiles()
        );

        assertEquals(version, payment.businessVersion());
        assertEquals(eventCount, payment.domainEvents().size());
    }

    @Test
    void secondPostingInstructionIsRejectedWithoutMutation() {
        Payment payment =
                PaymentAggregateTestFixtures.postingPendingPayment();
        PaymentState before = payment.toState();
        int eventCount = payment.domainEvents().size();

        PostingInstructionIdentity conflicting =
                new PostingInstructionIdentity(
                        new com.sixpay.payment.domain.model.evidence
                                .PostingInstructionId(
                                UUID.fromString(
                                        "24738057-92c7-4437-918a-2b88d128c22d"
                                )
                        ),
                        new com.sixpay.payment.domain.model.evidence
                                .PostingIdempotencyKey(
                                "POSTING-IDEMPOTENCY-002"
                        ),
                        PaymentAggregateTestFixtures.AMOUNT,
                        PaymentAggregateTestFixtures
                                .ACCOUNT_FINGERPRINT,
                        PaymentAggregateTestFixtures.fingerprint("9")
                );

        assertThrows(
                PaymentDomainException.class,
                () -> payment.authorizePosting(
                        conflicting,
                        PaymentAggregateTestFixtures.T0
                                .plusSeconds(7),
                        PaymentAggregateTestFixtures.profiles()
                )
        );

        assertEquals(before, payment.toState());
        assertEquals(eventCount, payment.domainEvents().size());
    }
}
