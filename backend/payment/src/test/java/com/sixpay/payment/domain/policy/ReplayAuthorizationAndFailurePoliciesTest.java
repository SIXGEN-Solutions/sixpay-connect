package com.sixpay.payment.domain.policy;

import com.sixpay.payment.domain.model.*;
import com.sixpay.payment.domain.model.evidence.*;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReplayAuthorizationAndFailurePoliciesTest {

    @Test
    void replayPolicyHandlesNewIdenticalReplacementAndTerminalConflict() {
        EvidenceReplayReplacementPolicy policy =
                new EvidenceReplayReplacementPolicy();

        EvidenceIdentity candidate = new EvidenceIdentity(
                "evidence-1",
                EvidenceFingerprint.of(
                        "v1:sha256:" + "a".repeat(64)
                )
        );

        assertEquals(
                EvidenceReplayDecision.ACCEPT_NEW,
                policy.decide(
                        null,
                        candidate,
                        null,
                        EvidenceAuthority.DIRECT_RESPONSE,
                        null,
                        EvidenceConclusiveness.PARTIAL,
                        PaymentLifecycleContext.of(
                                PaymentStatus.POSTING_PENDING
                        )
                ).decision()
        );

        assertEquals(
                EvidenceReplayDecision.NO_OP_IDENTICAL,
                policy.decide(
                        candidate,
                        candidate,
                        EvidenceAuthority.DIRECT_RESPONSE,
                        EvidenceAuthority.DIRECT_RESPONSE,
                        EvidenceConclusiveness.PARTIAL,
                        EvidenceConclusiveness.PARTIAL,
                        PaymentLifecycleContext.of(
                                PaymentStatus.POSTING_PENDING
                        )
                ).decision()
        );

        EvidenceIdentity moreConclusive = new EvidenceIdentity(
                "evidence-2",
                EvidenceFingerprint.of(
                        "v1:sha256:" + "b".repeat(64)
                )
        );

        assertEquals(
                EvidenceReplayDecision.REPLACE_MORE_AUTHORITATIVE,
                policy.decide(
                        candidate,
                        moreConclusive,
                        EvidenceAuthority.DIRECT_RESPONSE,
                        EvidenceAuthority.BANK_REFERENCE_LOOKUP,
                        EvidenceConclusiveness.PARTIAL,
                        EvidenceConclusiveness.CONCLUSIVE,
                        PaymentLifecycleContext.of(
                                PaymentStatus.POSTING_OUTCOME_UNKNOWN
                        )
                ).decision()
        );

        assertEquals(
                EvidenceReplayDecision.REJECT_TERMINAL_REPLACEMENT,
                policy.decide(
                        candidate,
                        moreConclusive,
                        EvidenceAuthority.DIRECT_RESPONSE,
                        EvidenceAuthority.BANK_REFERENCE_LOOKUP,
                        EvidenceConclusiveness.PARTIAL,
                        EvidenceConclusiveness.FINAL,
                        PaymentLifecycleContext.of(
                                PaymentStatus.TREASURY_INTEGRATED
                        )
                ).decision()
        );
    }

    @Test
    void postingAuthorizationAllowsOnlyOneCoherentInstruction() {
        PostingInstructionAuthorizationPolicy policy =
                new PostingInstructionAuthorizationPolicy();
        PostingInstructionIdentity candidate =
                new PostingInstructionIdentity(
                        new PostingInstructionId(UUID.randomUUID()),
                        new PostingIdempotencyKey(
                                "POSTING-IDEMPOTENCY-001"
                        ),
                        PolicyTestFixtures.amount(),
                        "v1:" + "a".repeat(64)
                );

        assertEquals(
                PostingAuthorizationDecision.AUTHORIZE,
                policy.decide(
                        new PaymentPostingAuthorizationContext(
                                PaymentStatus.APPROVED_FOR_POSTING,
                                true,
                                true,
                                true,
                                true,
                                true,
                                null
                        ),
                        candidate,
                        PolicyTestFixtures.DECISION_AT,
                        PolicyTestFixtures
                                .postingAuthorizationProfile()
                ).decision()
        );

        assertEquals(
                PostingAuthorizationDecision.NO_OP_SAME_INSTRUCTION,
                policy.decide(
                        new PaymentPostingAuthorizationContext(
                                PaymentStatus.APPROVED_FOR_POSTING,
                                true,
                                true,
                                true,
                                true,
                                true,
                                candidate.instructionId()
                        ),
                        candidate,
                        PolicyTestFixtures.DECISION_AT,
                        PolicyTestFixtures
                                .postingAuthorizationProfile()
                ).decision()
        );
    }

    @Test
    void failurePolicyNeverReturnsFailedForUnknownEffect() {
        PolicyDecision<FailureDispositionDecision> decision =
                new FailureClassificationPolicy().decide(
                        new PaymentFailure(
                                FailureCode.of("BANK_TIMEOUT"),
                                FailureCategory
                                        .UNCERTAIN_EXTERNAL_OUTCOME,
                                FailureStage.POSTING,
                                RetryDisposition
                                        .AUTHORITATIVE_LOOKUP_REQUIRED,
                                "Authoritative lookup required",
                                PolicyTestFixtures.DECISION_AT,
                                ExternalSystem.AMPLITUDE
                        ),
                        FinancialEffectKnowledge.UNCERTAIN,
                        PaymentStatus.POSTING_OUTCOME_UNKNOWN,
                        PolicyTestFixtures.failureProfile()
                );

        assertEquals(
                FailureDispositionDecision.OUTCOME_UNKNOWN,
                decision.decision()
        );
    }
}
