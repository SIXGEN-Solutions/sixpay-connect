package com.sixpay.accounting.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.sixpay.accounting.domain.policy.AccountingSelectionWindow;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AccountingT1OperationalSnapshotTest {

    @Test
    void shouldMapAccountingCandidateAndTresorPayEvidence() {
        Instant checkedAt = Instant.parse("2026-09-10T12:00:00Z");
        AccountingCandidateProjection candidate = candidate(
                null,
                new TresorPayPaymentStatusEvidence(
                        "PAY-001",
                        "TP-001",
                        "COMPLETED",
                        "BANK",
                        null,
                        true,
                        true,
                        checkedAt.minusSeconds(30),
                        null,
                        checkedAt,
                        "PAY-001",
                        "corr-001"
                )
        );
        AccountingSelectionWindow window = new AccountingSelectionWindow(
                LocalDate.of(2026, 9, 10),
                Instant.parse("2026-09-09T23:00:00Z"),
                Instant.parse("2026-09-10T23:00:00Z")
        );

        AccountingT1OperationalSnapshot snapshot = AccountingT1OperationalSnapshot.from(
                candidate,
                window,
                AccountingT1OperationalCandidateStatus.ELIGIBLE_FOR_BATCH,
                AccountingT1EligibilityReason.NONE,
                AccountingT1TechnicalIssue.NONE
        );

        assertEquals(candidate.id(), snapshot.candidateId());
        assertEquals("COMPLETED", snapshot.tresorPayProviderStatus());
        assertEquals(checkedAt, snapshot.tresorPayCheckedAt());
        assertEquals(window.businessDate(), snapshot.selectionBusinessDate());
    }

    @Test
    void shouldRequireBatchIdWhenAssignedToBatch() {
        AccountingCandidateProjection candidate = candidate(null, null);
        AccountingSelectionWindow window = new AccountingSelectionWindow(
                LocalDate.of(2026, 9, 10),
                Instant.parse("2026-09-09T23:00:00Z"),
                Instant.parse("2026-09-10T23:00:00Z")
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> AccountingT1OperationalSnapshot.from(
                        candidate,
                        window,
                        AccountingT1OperationalCandidateStatus.ASSIGNED_TO_BATCH,
                        AccountingT1EligibilityReason.NONE,
                        AccountingT1TechnicalIssue.NONE
                )
        );
    }

    private static AccountingCandidateProjection candidate(
            UUID batchId,
            TresorPayPaymentStatusEvidence evidence
    ) {
        Instant now = Instant.parse("2026-09-10T10:00:00Z");
        return new AccountingCandidateProjection(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "PAY-001",
                "PARTNER-001",
                "LRB",
                "BANK-001",
                now,
                LocalDate.of(2026, 9, 10),
                UUID.randomUUID(),
                "v1",
                now,
                "DEBTOR-001",
                "CREDITOR-001",
                new BigDecimal("1000.00"),
                Currency.getInstance("XAF"),
                now,
                now,
                batchId,
                evidence,
                List.of()
        );
    }
}
