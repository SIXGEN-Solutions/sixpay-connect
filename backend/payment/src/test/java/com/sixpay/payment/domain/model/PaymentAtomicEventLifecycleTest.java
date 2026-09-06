package com.sixpay.payment.domain.model;

import com.sixpay.payment.domain.model.evidence.*;
import com.sixpay.payment.domain.policy.PostingInstructionIdentity;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PaymentAtomicEventLifecycleTest {

    @Test
    void atomicUnknownOutcomeMovesPaymentToPostingOutcomeUnknown() {
        Payment payment = PaymentAggregateTestFixtures.approvedPayment();
        PostingInstructionIdentity instruction =
                PaymentAggregateTestFixtures.postingInstruction();

        payment.authorizePaymentEventPosting(
                instruction,
                PaymentAggregateTestFixtures.T0.plusSeconds(6)
        );

        payment.recordPaymentEventOutcome(
                outcome(
                        instruction,
                        PaymentEventOutcome.UNKNOWN,
                        PaymentEventObservationSource.DIRECT_RESPONSE,
                        false
                ),
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
        assertEquals(
                PaymentEventOutcome.UNKNOWN,
                payment.toState()
                        .paymentEventOutcomeEvidence()
                        .orElseThrow()
                        .outcome()
        );
    }

    @Test
    void authoritativeCompletedLookupResolvesUnknownToPostedPendingTfj() {
        Payment payment = PaymentAggregateTestFixtures.approvedPayment();
        PostingInstructionIdentity instruction =
                PaymentAggregateTestFixtures.postingInstruction();

        payment.authorizePaymentEventPosting(
                instruction,
                PaymentAggregateTestFixtures.T0.plusSeconds(6)
        );

        payment.recordPaymentEventOutcome(
                outcome(
                        instruction,
                        PaymentEventOutcome.UNKNOWN,
                        PaymentEventObservationSource.DIRECT_RESPONSE,
                        false
                ),
                PaymentAggregateTestFixtures.unknownFailure(
                        "POSTING_OUTCOME_UNKNOWN",
                        FailureStage.POSTING,
                        PaymentAggregateTestFixtures.T0.plusSeconds(7)
                ),
                PaymentAggregateTestFixtures.T0.plusSeconds(7),
                PaymentAggregateTestFixtures.profiles()
        );

        payment.resolvePaymentEventOutcome(
                outcome(
                        instruction,
                        PaymentEventOutcome.COMPLETED,
                        PaymentEventObservationSource.IDEMPOTENCY_LOOKUP,
                        true
                ),
                null,
                PaymentAggregateTestFixtures.T0.plusSeconds(8),
                PaymentAggregateTestFixtures.profiles()
        );

        assertEquals(
                PaymentStatus.POSTED_PENDING_TFJ,
                payment.status()
        );
    }

    private static PaymentEventOutcomeSnapshot outcome(
            PostingInstructionIdentity instruction,
            PaymentEventOutcome outcome,
            PaymentEventObservationSource source,
            boolean completed
    ) {
        List<FundsControlCheckEvidence> checks =
                Arrays.stream(FundsControlCheckType.values())
                        .map(type ->
                                new FundsControlCheckEvidence(
                                        type,
                                        completed
                                                ? EvidenceCheckResult.PASS
                                                : EvidenceCheckResult.UNKNOWN,
                                        null,
                                        PaymentAggregateTestFixtures.T0
                                                .plusSeconds(7)
                                )
                        )
                        .toList();

        return new PaymentEventOutcomeSnapshot(
                instruction.instructionId(),
                instruction.idempotencyKey(),
                outcome,
                checks,
                completed
                        ? BankPostingReference.principalOnly("BANK-REF-1")
                        : null,
                null,
                LocalDate.of(2026, 9, 6),
                PaymentAggregateTestFixtures.T0.plusSeconds(7),
                source
        );
    }
}
