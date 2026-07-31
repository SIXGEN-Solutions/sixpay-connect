package com.sixpay.payment.domain.model.evidence;

import com.sixpay.payment.domain.model.BankPostingReference;
import com.sixpay.payment.domain.model.ExternalSystem;
import com.sixpay.payment.domain.model.FailureCode;
import com.sixpay.payment.domain.model.FinancialInstitutionCode;
import com.sixpay.payment.domain.model.PublicPaymentReference;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EndOfDayAndReversalSnapshotsTest {

    @Test
    void integratedTfjEvidenceContainsNoFailure() {
        EndOfDayConfirmationSnapshot snapshot =
                tfj(
                        TfjStatus.INTEGRATED,
                        null,
                        Instant.parse("2026-07-31T17:00:00Z"),
                        Instant.parse("2026-07-31T17:01:00Z")
                );

        assertEquals(TfjStatus.INTEGRATED, snapshot.tfjStatus());
        assertFalse(snapshot.failureEvidence().isPresent());
    }

    @Test
    void failedTfjEvidenceRequiresRecoveryAndValidChronology() {
        tfj(
                TfjStatus.FAILED,
                new TfjFailureEvidence(
                        FailureCode.of("TFJ_INTEGRATION_FAILED"),
                        TfjRecoveryAction.REVERSAL_REVIEW
                ),
                Instant.parse("2026-07-31T17:00:00Z"),
                Instant.parse("2026-07-31T17:01:00Z")
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> tfj(
                        TfjStatus.FAILED,
                        null,
                        Instant.parse("2026-07-31T17:00:00Z"),
                        Instant.parse("2026-07-31T17:01:00Z")
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> tfj(
                        TfjStatus.INTEGRATED,
                        null,
                        Instant.parse("2026-07-31T17:01:00Z"),
                        Instant.parse("2026-07-31T17:00:00Z")
                )
        );
    }

    @Test
    void reversalAuthorizationPrecedesSubmission() {
        ReversalAuthorizationEvidence authorization = authorization();

        assertEquals(
                ReversalAuthorizationType.APPROVED_RUNBOOK,
                authorization.authorizationType()
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new ReversalAuthorizationEvidence(
                        ReversalAuthorizationType.APPROVED_RUNBOOK,
                        new ReversalAuthorizationReference(
                                "RUNBOOK-AUTH-0001"
                        ),
                        "operator:payments",
                        FailureCode.of("TFJ_REVERSAL_REQUIRED"),
                        Instant.parse("2026-07-31T18:01:00Z"),
                        Instant.parse("2026-07-31T18:00:00Z")
                )
        );
    }

    @Test
    void reversalSnapshotPreservesOriginalInstructionAndAuthorization() {
        ReversalSnapshot pending = new ReversalSnapshot(
                originalPosting(),
                new ReversalInstructionId(UUID.randomUUID()),
                new ReversalIdempotencyKey(
                        "REVERSAL-IDEMPOTENCY-0001"
                ),
                authorization(),
                null
        );

        ReversalSnapshot resolved = pending.withOutcome(
                reversedOutcome()
        );

        assertFalse(pending.outcome().isPresent());
        assertEquals(
                pending.originalBankPostingReference(),
                resolved.originalBankPostingReference()
        );
        assertEquals(
                pending.reversalInstructionId(),
                resolved.reversalInstructionId()
        );
        assertEquals(
                pending.authorization(),
                resolved.authorization()
        );
        assertEquals(
                ReversalOutcome.REVERSED,
                resolved.outcome().orElseThrow().outcome()
        );
    }

    @Test
    void reversalOutcomeMatrixIsStrict() {
        reversedOutcome();

        new ReversalOutcomeEvidence(
                null,
                ReversalOutcome.REJECTED,
                null,
                FailureCode.of("REVERSAL_REJECTED"),
                reversalMetadata()
        );

        new ReversalOutcomeEvidence(
                null,
                ReversalOutcome.UNKNOWN,
                null,
                null,
                reversalMetadata()
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new ReversalOutcomeEvidence(
                        null,
                        ReversalOutcome.REVERSED,
                        null,
                        null,
                        reversalMetadata()
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new ReversalOutcomeEvidence(
                        null,
                        ReversalOutcome.NOT_ALLOWED,
                        null,
                        null,
                        reversalMetadata()
                )
        );
    }

    private static EndOfDayConfirmationSnapshot tfj(
            TfjStatus status,
            TfjFailureEvidence failure,
            Instant confirmedAt,
            Instant matchedAt
    ) {
        return new EndOfDayConfirmationSnapshot(
                new TfjConfirmationId(UUID.randomUUID()),
                FinancialInstitutionCode.of("BANK_CM"),
                LocalDate.of(2026, 7, 31),
                PublicPaymentReference.of(
                        "PAY-01J8YH6M6VT8EF3Z7Q4N9P2KDC"
                ),
                "POSTING-01",
                "TFJ-BATCH-01",
                status,
                failure,
                confirmedAt,
                matchedAt,
                EvidenceMetadataTest.metadata(
                        ExternalSystem.AMPLITUDE,
                        EvidenceObservationChannel.ASYNC_CALLBACK
                )
        );
    }

    private static ReversalAuthorizationEvidence authorization() {
        return new ReversalAuthorizationEvidence(
                ReversalAuthorizationType.APPROVED_RUNBOOK,
                new ReversalAuthorizationReference(
                        "RUNBOOK-AUTH-0001"
                ),
                "operator:payments",
                FailureCode.of("TFJ_REVERSAL_REQUIRED"),
                Instant.parse("2026-07-31T17:59:00Z"),
                Instant.parse("2026-07-31T18:00:00Z")
        );
    }

    private static ReversalOutcomeEvidence reversedOutcome() {
        return new ReversalOutcomeEvidence(
                new ReversalReference("REV-0001"),
                ReversalOutcome.REVERSED,
                "REVERSAL-ENTRY-01",
                null,
                reversalMetadata()
        );
    }

    private static EvidenceMetadata reversalMetadata() {
        return EvidenceMetadataTest.metadata(
                ExternalSystem.AMPLITUDE,
                EvidenceObservationChannel.BANK_REFERENCE_LOOKUP
        );
    }

    private static BankPostingReference originalPosting() {
        return new BankPostingReference(
                "POSTING-01",
                "DEBIT-01",
                "CUT-01"
        );
    }
}
