package com.sixpay.payment.domain.model.evidence;

import com.sixpay.payment.domain.model.BankPostingReference;
import com.sixpay.payment.domain.model.ExternalSystem;
import com.sixpay.payment.domain.model.FailureCode;
import com.sixpay.payment.domain.model.FinancialInstitutionCode;
import com.sixpay.payment.domain.model.TreasuryAccountReference;
import com.sixpay.sharedkernel.domain.valueobject.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TreasuryAndPostingSnapshotsTest {

    @Test
    void resolvedTreasuryEvidenceRequiresProtectedConfiguration() {
        TreasuryAccountResolutionSnapshot snapshot =
                new TreasuryAccountResolutionSnapshot(
                        treasuryAccount(),
                        EvidenceMetadataTest.fingerprint("b"),
                        TreasuryResolutionOutcome.RESOLVED,
                        "policy-v7",
                        null,
                        treasuryMetadata()
                );

        assertEquals(
                TreasuryResolutionOutcome.RESOLVED,
                snapshot.resolutionOutcome()
        );
        assertFalse(snapshot.rejectionCode().isPresent());

        assertThrows(
                IllegalArgumentException.class,
                () -> new TreasuryAccountResolutionSnapshot(
                        null,
                        EvidenceMetadataTest.fingerprint("b"),
                        TreasuryResolutionOutcome.RESOLVED,
                        "policy-v7",
                        null,
                        treasuryMetadata()
                )
        );
    }

    @Test
    void rejectedTreasuryEvidenceHasNoResolvedAccount() {
        new TreasuryAccountResolutionSnapshot(
                null,
                EvidenceMetadataTest.fingerprint("b"),
                TreasuryResolutionOutcome.REJECTED,
                "policy-v7",
                FailureCode.of("TREASURY_ACCOUNT_NOT_RESOLVED"),
                treasuryMetadata()
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new TreasuryAccountResolutionSnapshot(
                        treasuryAccount(),
                        EvidenceMetadataTest.fingerprint("b"),
                        TreasuryResolutionOutcome.REJECTED,
                        "policy-v7",
                        FailureCode.of("TREASURY_ACCOUNT_NOT_RESOLVED"),
                        treasuryMetadata()
                )
        );
    }

    @Test
    void completedPostingRequiresBothSuccessfulLegsAndReference() {
        PostingOutcomeSnapshot snapshot =
                posting(
                        PostingOutcome.COMPLETED,
                        bankReference(),
                        successfulLeg("DEBIT-01"),
                        successfulLeg("CUT-01"),
                        null,
                        PostingNextAction.NONE
                );

        assertEquals(PostingOutcome.COMPLETED, snapshot.outcome());

        assertThrows(
                IllegalArgumentException.class,
                () -> posting(
                        PostingOutcome.COMPLETED,
                        null,
                        successfulLeg("DEBIT-01"),
                        successfulLeg("CUT-01"),
                        null,
                        PostingNextAction.NONE
                )
        );
    }

    @Test
    void rejectedPostingProvesNoFinancialEffect() {
        posting(
                PostingOutcome.REJECTED_NO_FINANCIAL_EFFECT,
                null,
                failedLeg("DEBIT_REJECTED"),
                new PostingLegEvidence(
                        PostingLegStatus.NOT_STARTED,
                        null,
                        null,
                        null
                ),
                FailureCode.of("POSTING_REJECTED"),
                PostingNextAction.NONE
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> posting(
                        PostingOutcome.REJECTED_NO_FINANCIAL_EFFECT,
                        bankReference(),
                        successfulLeg("DEBIT-01"),
                        new PostingLegEvidence(
                                PostingLegStatus.NOT_STARTED,
                                null,
                                null,
                                null
                        ),
                        FailureCode.of("POSTING_REJECTED"),
                        PostingNextAction.NONE
                )
        );
    }

    @Test
    void debitOnlyAndUnknownOutcomesStayDistinct() {
        posting(
                PostingOutcome.DEBIT_CONFIRMED_CUT_CREDIT_PENDING,
                bankReference(),
                successfulLeg("DEBIT-01"),
                new PostingLegEvidence(
                        PostingLegStatus.PENDING,
                        null,
                        null,
                        null
                ),
                null,
                PostingNextAction.WAIT_FOR_CUT_CREDIT
        );

        posting(
                PostingOutcome.UNKNOWN,
                null,
                new PostingLegEvidence(
                        PostingLegStatus.UNKNOWN,
                        null,
                        null,
                        null
                ),
                new PostingLegEvidence(
                        PostingLegStatus.UNKNOWN,
                        null,
                        null,
                        null
                ),
                null,
                PostingNextAction.QUERY_OUTCOME
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> posting(
                        PostingOutcome.UNKNOWN,
                        bankReference(),
                        successfulLeg("DEBIT-01"),
                        successfulLeg("CUT-01"),
                        null,
                        PostingNextAction.QUERY_OUTCOME
                )
        );
    }

    @Test
    void reversalRequiredNeedsConfirmedEffectAndExplicitAction() {
        posting(
                PostingOutcome.REVERSAL_REQUIRED,
                bankReference(),
                successfulLeg("DEBIT-01"),
                failedLeg("CUT_CREDIT_FAILED"),
                FailureCode.of("CUT_CREDIT_FAILED"),
                PostingNextAction.REQUEST_EXPLICIT_REVERSAL
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> posting(
                        PostingOutcome.REVERSAL_REQUIRED,
                        bankReference(),
                        failedLeg("DEBIT_FAILED"),
                        failedLeg("CUT_FAILED"),
                        FailureCode.of("POSTING_FAILED"),
                        PostingNextAction.REQUEST_EXPLICIT_REVERSAL
                )
        );
    }

    @Test
    void bankAndLegReferencesMustBeConsistent() {
        assertThrows(
                IllegalArgumentException.class,
                () -> posting(
                        PostingOutcome.COMPLETED,
                        bankReference(),
                        successfulLeg("OTHER-DEBIT"),
                        successfulLeg("CUT-01"),
                        null,
                        PostingNextAction.NONE
                )
        );
    }

    private static PostingOutcomeSnapshot posting(
            PostingOutcome outcome,
            BankPostingReference bankReference,
            PostingLegEvidence debit,
            PostingLegEvidence cut,
            FailureCode rejectionCode,
            PostingNextAction nextAction
    ) {
        return new PostingOutcomeSnapshot(
                new PostingInstructionId(UUID.randomUUID()),
                new PostingIdempotencyKey("POSTING-IDEMPOTENCY-0001"),
                outcome,
                bankReference,
                debit,
                cut,
                Money.of(new BigDecimal("1000"), "XAF"),
                LocalDate.of(2026, 7, 31),
                rejectionCode,
                nextAction,
                EvidenceMetadataTest.metadata(
                        ExternalSystem.AMPLITUDE,
                        EvidenceObservationChannel.DIRECT_RESPONSE
                )
        );
    }

    private static PostingLegEvidence successfulLeg(String reference) {
        return new PostingLegEvidence(
                PostingLegStatus.SUCCEEDED,
                reference,
                Instant.parse("2026-07-31T10:00:00Z"),
                null
        );
    }

    private static PostingLegEvidence failedLeg(String code) {
        return new PostingLegEvidence(
                PostingLegStatus.FAILED,
                null,
                null,
                FailureCode.of(code)
        );
    }

    private static BankPostingReference bankReference() {
        return new BankPostingReference(
                "POSTING-01",
                "DEBIT-01",
                "CUT-01"
        );
    }

    private static TreasuryAccountReference treasuryAccount() {
        return new TreasuryAccountReference(
                FinancialInstitutionCode.of("BANK_CM"),
                "CUT-CONFIG-01",
                "vault:cut:0001",
                "****************9999",
                "v7"
        );
    }

    private static EvidenceMetadata treasuryMetadata() {
        return EvidenceMetadataTest.metadata(
                ExternalSystem.SIXPAY,
                EvidenceObservationChannel.PROTECTED_CONFIGURATION_RESOLUTION
        );
    }
}
