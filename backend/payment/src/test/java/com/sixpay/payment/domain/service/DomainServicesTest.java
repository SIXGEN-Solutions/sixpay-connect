package com.sixpay.payment.domain.service;

import com.sixpay.payment.domain.model.*;
import com.sixpay.payment.domain.model.evidence.*;
import com.sixpay.payment.domain.policy.*;
import com.sixpay.sharedkernel.domain.valueobject.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DomainServicesTest {

    private static final Instant NOW =
            Instant.parse("2026-07-31T12:00:00Z");

    @Test
    void postingServiceReturnsTypedDecisionWithoutMutation() {
        PostingInstructionId instructionId =
                new PostingInstructionId(UUID.randomUUID());
        PostingIdempotencyKey key =
                new PostingIdempotencyKey(
                        "POSTING-IDEMPOTENCY-001"
                );
        Money amount = Money.of(
                new BigDecimal("1000"),
                "XAF"
        );

        PostingOutcomeSnapshot snapshot =
                new PostingOutcomeSnapshot(
                        instructionId,
                        key,
                        PostingOutcome.COMPLETED,
                        new BankPostingReference(
                                "POSTING-001",
                                "DEBIT-001",
                                "CUT-001"
                        ),
                        new PostingLegEvidence(
                                PostingLegStatus.SUCCEEDED,
                                "DEBIT-001",
                                NOW,
                                null
                        ),
                        new PostingLegEvidence(
                                PostingLegStatus.SUCCEEDED,
                                "CUT-001",
                                NOW,
                                null
                        ),
                        amount,
                        LocalDate.of(2026, 7, 31),
                        null,
                        PostingNextAction.NONE,
                        metadata(
                                EvidenceObservationChannel
                                        .DIRECT_RESPONSE
                        )
                );

        PolicyDecision<PostingDecision> decision =
                new PostingOutcomeDecisionService().decide(
                        new PostingDecisionInput(
                                new PaymentPostingContext(
                                        PaymentStatus.POSTING_PENDING,
                                        instructionId,
                                        key,
                                        amount
                                ),
                                snapshot,
                                null,
                                null,
                                EvidenceAuthority.DIRECT_RESPONSE,
                                EvidenceConclusiveness.CONCLUSIVE,
                                PaymentLifecycleContext.of(
                                        PaymentStatus.POSTING_PENDING
                                ),
                                NOW
                        ),
                        bundle()
                );

        assertEquals(
                PostingDecision.POSTED_PENDING_TFJ,
                decision.decision()
        );
    }

    @Test
    void endOfDayServiceRequiresConclusiveUniqueMatch() {
        TfjConfirmationId confirmationId =
                new TfjConfirmationId(UUID.randomUUID());

        EndOfDayConfirmationSnapshot snapshot =
                new EndOfDayConfirmationSnapshot(
                        confirmationId,
                        FinancialInstitutionCode.of("BANK_CM"),
                        LocalDate.of(2026, 7, 31),
                        PublicPaymentReference.of(
                                "PAY-01J8YH6M6VT8EF3Z7Q4N9P2KDC"
                        ),
                        "POSTING-001",
                        "TFJ-BATCH-001",
                        TfjStatus.INTEGRATED,
                        null,
                        NOW.minusSeconds(30),
                        NOW.minusSeconds(20),
                        metadata(
                                EvidenceObservationChannel.ASYNC_CALLBACK
                        )
                );

        PolicyDecision<EndOfDayDecision> decision =
                new EndOfDayDecisionService().decide(
                        new EndOfDayDecisionInput(
                                new PaymentTfjContext(
                                        FinancialInstitutionCode.of(
                                                "BANK_CM"
                                        ),
                                        LocalDate.of(2026, 7, 31),
                                        PublicPaymentReference.of(
                                                "PAY-01J8YH6M6VT8EF3Z7Q4N9P2KDC"
                                        ),
                                        "POSTING-001"
                                ),
                                snapshot,
                                new UniqueTfjMatchProof(
                                        confirmationId,
                                        true,
                                        true,
                                        true
                                ),
                                null,
                                null,
                                EvidenceAuthority.UNIQUE_TFJ_MATCH,
                                EvidenceConclusiveness.FINAL,
                                PaymentLifecycleContext.of(
                                        PaymentStatus.POSTED_PENDING_TFJ
                                ),
                                NOW
                        ),
                        bundle()
                );

        assertEquals(
                EndOfDayDecision.TREASURY_INTEGRATED,
                decision.decision()
        );
    }

    @Test
    void resultIntentServiceDelegatesToPurePolicy() {
        PolicyDecision<ResultIntentDecision> decision =
                new PaymentResultIntentService().decide(
                        new PaymentResultContext(
                                PublicPaymentReference.of(
                                        "PAY-01J8YH6M6VT8EF3Z7Q4N9P2KDC"
                                ),
                                "correlation-001"
                        ),
                        PaymentStatus.REVERSAL_PENDING,
                        PaymentStatus.REVERSED,
                        null,
                        NOW,
                        bundle()
                );

        assertEquals(
                ResultIntentDecision.REVERSAL_REVERSED,
                decision.decision()
        );
    }

    private static EvidenceMetadata metadata(
            EvidenceObservationChannel channel
    ) {
        return new EvidenceMetadata(
                ExternalSystem.AMPLITUDE,
                com.sixpay.common.context.CorrelationId.of(
                        "40a11cb8-b32c-474e-bab2-e0b6f43138c8"
                ),
                channel,
                EvidenceFingerprint.of(
                        "v1:sha256:" + "a".repeat(64)
                ),
                NOW.minusSeconds(60),
                NOW.minusSeconds(50)
        );
    }

    private static PaymentPolicyBundle bundle() {
        PolicyProfileMetadata metadata =
                new PolicyProfileMetadata(
                        "profile",
                        "v1",
                        Instant.parse("2026-01-01T00:00:00Z"),
                        null,
                        "approval:board"
                );

        Map<EvidenceCategory, java.time.Duration> ages =
                new java.util.EnumMap<>(EvidenceCategory.class);
        for (EvidenceCategory category : EvidenceCategory.values()) {
            ages.put(category, java.time.Duration.ofHours(1));
        }

        EvidenceTemporalProfile temporal =
                new EvidenceTemporalProfile(
                        metadata,
                        java.time.Duration.ofMinutes(1),
                        ages
                );

        return new PaymentPolicyBundle(
                temporal,
                new AuthorizationPolicyProfile(
                        metadata,
                        Set.of("issuer"),
                        Set.of("RS256"),
                        Set.of("payment:initiate"),
                        Set.of(AuthorizationBindingType.PAYMENT_SCOPE)
                ),
                new BankingVerificationPolicyProfile(
                        metadata,
                        Set.of(
                                BankingVerificationCheckType
                                        .CUSTOMER_EXISTS
                        ),
                        temporal
                ),
                new FundsControlPolicyProfile(
                        metadata,
                        Set.of(
                                FundsControlCheckType.ACCOUNT_EXISTS
                        ),
                        temporal
                ),
                new TreasuryResolutionPolicyProfile(
                        metadata,
                        Map.of(
                                FinancialInstitutionCode.of("BANK_CM"),
                                Set.of("v7")
                        )
                ),
                new PostingAuthorizationPolicyProfile(
                        metadata,
                        Set.of(PaymentStatus.APPROVED_FOR_POSTING),
                        true,
                        true
                ),
                new FinancialOutcomePolicyProfile(
                        metadata,
                        Map.of(
                                EvidenceAuthority.DIRECT_RESPONSE, 1,
                                EvidenceAuthority.IDEMPOTENCY_LOOKUP, 2,
                                EvidenceAuthority.BANK_REFERENCE_LOOKUP, 3,
                                EvidenceAuthority.UNIQUE_TFJ_MATCH, 4
                        ),
                        Map.of(
                                EvidenceConclusiveness.INDETERMINATE, 1,
                                EvidenceConclusiveness.PARTIAL, 2,
                                EvidenceConclusiveness.CONCLUSIVE, 3,
                                EvidenceConclusiveness.FINAL, 4
                        )
                ),
                new TfjPolicyProfile(
                        metadata,
                        Set.of(TfjStatus.INTEGRATED, TfjStatus.FAILED),
                        Set.of(TfjRecoveryAction.REVERSAL_REQUIRED)
                ),
                new ReversalPolicyProfile(
                        metadata,
                        Set.of(
                                ReversalAuthorizationType.APPROVED_RUNBOOK
                        ),
                        Set.of("TFJ_REVERSAL_REQUIRED")
                ),
                new FailureClassificationProfile(
                        metadata,
                        Map.of()
                ),
                new ResultIntentPolicyProfile(
                        metadata,
                        Map.of(
                                PaymentStatus.POSTED_PENDING_TFJ,
                                ResultIntentDecision
                                        .IMMEDIATE_POSTED_PENDING_TFJ,
                                PaymentStatus.REVERSED,
                                ResultIntentDecision.REVERSAL_REVERSED
                        )
                ),
                new EventDisclosureProfile(
                        metadata,
                        Map.of(),
                        Map.of(),
                        Set.of(),
                        Set.of(EventDataClassification.PUBLIC)
                )
        );
    }
}
