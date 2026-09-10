package com.sixpay.accounting.application.service;

import com.sixpay.accounting.domain.model.AccountingBatchItem;
import com.sixpay.accounting.domain.model.AccountingPaymentCandidate;
import com.sixpay.accounting.domain.model.TresorPayPaymentStatusEvidence;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

class AccountingBatchSnapshotContentTest {
    @Test
    void batchItemCopiesFinalizedFinancialSnapshotFacts() {
        UUID snapshotId = UUID.randomUUID();
        AccountingPaymentCandidate candidate = new AccountingPaymentCandidate(
                UUID.randomUUID(),
                "PAY-T13-001",
                "TRESORPAY",
                "LAREGIONALE",
                snapshotId,
                "v1",
                Instant.parse("2026-08-11T11:59:00Z"),
                "DEBTOR-001",
                "TREASURY-001",
                new BigDecimal("1000.00"),
                Currency.getInstance("XAF"),
                Instant.parse("2026-08-11T11:58:00Z"),
                LocalDate.of(2026, 8, 11),
                "RB-T13-001",
                new TresorPayPaymentStatusEvidence(
                        "PAY-T13-001", "TX-T13-001", "COMPLETED", "BANK_TRANSFER", "RB-T13-001",
                        true, true, Instant.parse("2026-08-11T11:58:30Z"), null,
                        Instant.parse("2026-08-11T12:00:00Z"), "PAY-T13-001", "corr-t13"),
                List.of(
                        new AccountingPaymentCandidate.FrozenEntry(UUID.randomUUID(), 1, "DEBIT", "DEBTOR-001",
                                new BigDecimal("1000.00"), Currency.getInstance("XAF"), Instant.parse("2026-08-11T11:59:01Z")),
                        new AccountingPaymentCandidate.FrozenEntry(UUID.randomUUID(), 2, "CREDIT", "TREASURY-001",
                                new BigDecimal("1000.00"), Currency.getInstance("XAF"), Instant.parse("2026-08-11T11:59:02Z"))
                )
        );

        AccountingBatchItem item = AccountingBatchItem.from(candidate);
        assertEquals(snapshotId, item.financialSnapshotId());
        assertEquals("v1", item.financialSnapshotVersion());
        assertEquals("DEBTOR-001", item.debtorAccountReference());
        assertEquals("TREASURY-001", item.creditorAccountReference());
        assertEquals(2, item.entries().size());
        assertEquals("DEBIT", item.entries().get(0).direction());
        assertEquals("CREDIT", item.entries().get(1).direction());

        assertEquals(candidate.entries().size(), item.entries().size());

        for (int index = 0; index < candidate.entries().size(); index++) {
            var sourceEntry = candidate.entries().get(index);
            var batchEntry = item.entries().get(index);

            assertEquals(sourceEntry.entrySnapshotId(), batchEntry.entrySnapshotId());
            assertEquals(sourceEntry.sequence(), batchEntry.sequence());
            assertEquals(sourceEntry.direction(), batchEntry.direction());
            assertEquals(sourceEntry.accountReference(), batchEntry.accountReference());
            assertEquals(sourceEntry.amount(), batchEntry.amount());
            assertEquals(sourceEntry.currency(), batchEntry.currency());
            assertEquals(sourceEntry.createdAt(), batchEntry.createdAt());
        }

        assertNotSame(candidate.entries(), item.entries());
    }
}
