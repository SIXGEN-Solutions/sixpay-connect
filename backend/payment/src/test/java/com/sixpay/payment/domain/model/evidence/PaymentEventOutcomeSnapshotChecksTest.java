package com.sixpay.payment.domain.model.evidence;

import com.sixpay.payment.domain.model.BankPostingReference;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentEventOutcomeSnapshotChecksTest {

    private static final Instant OBSERVED_AT = Instant.parse("2026-11-06T02:36:13Z");
    private static final LocalDate DCO = LocalDate.of(2026, 11, 5);

    @Test
    void completedAcceptsOnlyTheThreeAuthoritativeMandatoryChecks() {
        PaymentEventOutcomeSnapshot snapshot = snapshot(
                PaymentEventOutcome.COMPLETED, requiredChecks(), DCO
        );
        assertEquals(3, snapshot.checks().size());
    }

    @Test
    void completedAcceptsAdditionalPassingChecks() {
        PaymentEventOutcomeSnapshot snapshot = snapshot(
                PaymentEventOutcome.COMPLETED,
                Arrays.stream(FundsControlCheckType.values())
                        .map(type -> check(type, EvidenceCheckResult.PASS))
                        .toList(),
                DCO
        );
        assertEquals(FundsControlCheckType.values().length, snapshot.checks().size());
    }

    @Test
    void completedRejectsMissingMandatoryChecks() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> snapshot(PaymentEventOutcome.COMPLETED,
                        List.of(check(FundsControlCheckType.ACCOUNT_VALID,
                                EvidenceCheckResult.PASS)), DCO));
        assertTrue(error.getMessage().contains("missing required checks"));
    }

    @Test
    void completedRejectsExtraFailingChecks() {
        List<FundsControlCheckEvidence> checks = new java.util.ArrayList<>(requiredChecks());
        checks.add(check(FundsControlCheckType.CURRENCY_SUPPORTED, EvidenceCheckResult.FAIL));
        assertThrows(IllegalArgumentException.class,
                () -> snapshot(PaymentEventOutcome.COMPLETED, checks, DCO));
    }

    @Test
    void completedStillRequiresCoreBankingAccountingDate() {
        assertThrows(IllegalArgumentException.class,
                () -> snapshot(PaymentEventOutcome.COMPLETED, requiredChecks(), null));
    }

    @Test
    void duplicateTypesAreRejected() {
        List<FundsControlCheckEvidence> checks = new java.util.ArrayList<>(requiredChecks());
        checks.add(check(FundsControlCheckType.ACCOUNT_VALID, EvidenceCheckResult.PASS));
        assertThrows(IllegalArgumentException.class,
                () -> snapshot(PaymentEventOutcome.COMPLETED, checks, DCO));
    }

    @Test
    void rejectedAcceptsPartialChecksWithAtLeastOneFailure() {
        PaymentEventOutcomeSnapshot snapshot = snapshot(
                PaymentEventOutcome.REJECTED,
                List.of(check(FundsControlCheckType.DEBIT_ALLOWED, EvidenceCheckResult.FAIL)),
                null
        );
        assertEquals(1, snapshot.checks().size());
    }

    @Test
    void unknownAcceptsAnEmptyCheckListWithoutClaimingSuccess() {
        PaymentEventOutcomeSnapshot snapshot = snapshot(
                PaymentEventOutcome.UNKNOWN, List.of(), null
        );
        assertTrue(snapshot.checks().isEmpty());
    }

    private static List<FundsControlCheckEvidence> requiredChecks() {
        return List.of(
                check(FundsControlCheckType.ACCOUNT_VALID, EvidenceCheckResult.PASS),
                check(FundsControlCheckType.DEBIT_ALLOWED, EvidenceCheckResult.PASS),
                check(FundsControlCheckType.AVAILABLE_FUNDS_SUFFICIENT, EvidenceCheckResult.PASS)
        );
    }

    private static FundsControlCheckEvidence check(
            FundsControlCheckType type, EvidenceCheckResult result
    ) {
        return new FundsControlCheckEvidence(type, result, null, OBSERVED_AT);
    }

    private static PaymentEventOutcomeSnapshot snapshot(
            PaymentEventOutcome outcome,
            List<FundsControlCheckEvidence> checks,
            LocalDate dco
    ) {
        return new PaymentEventOutcomeSnapshot(
                new PostingInstructionId(UUID.fromString("80579bb5-af3c-46bd-a2fc-d30fb2672aed")),
                new PostingIdempotencyKey("POSTING-IDEMPOTENCY-001"),
                outcome,
                checks,
                outcome == PaymentEventOutcome.COMPLETED
                        ? BankPostingReference.principalOnly("787/000002") : null,
                null,
                dco,
                OBSERVED_AT,
                PaymentEventObservationSource.DIRECT_RESPONSE
        );
    }
}
