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
                UUID.randomUUID(), "PAY-T13-001", "TRESORPAY", "LAREGIONALE",
                new BigDecimal("1000.00"), Currency.getInstance("XAF"),
                Instant.parse("2026-08-11T11:58:00Z"), LocalDate.of(2026, 8, 11),
                "RB-T13-001", snapshotId, "v1",
                Instant.parse("2026-08-11T11:59:00Z"), "DEBTOR-001", "TREASURY-001",
                List.of(
                        new AccountingPaymentCandidate.FrozenEntry(UUID.randomUUID(), 1, "DEBIT", "DEBTOR-001",
                                new BigDecimal("1000.00"), Currency.getInstance("XAF"), Instant.parse("2026-08-11T11:59:01Z")),
                        new AccountingPaymentCandidate.FrozenEntry(UUID.randomUUID(), 2, "CREDIT", "TREASURY-001",
                                new BigDecimal("1000.00"), Currency.getInstance("XAF"), Instant.parse("2026-08-11T11:59:02Z"))
                ),
                new TresorPayPaymentStatusEvidence(
                        "PAY-T13-001", "TX-T13-001", "COMPLETED", "BANK_TRANSFER", "RB-T13-001",
                        true, true, Instant.parse("2026-08-11T11:58:30Z"), null,
                        Instant.parse("2026-08-11T12:00:00Z"), "PAY-T13-001", "corr-t13")
        );

        AccountingBatchItem item = AccountingBatchItem.from(candidate);
        assertEquals(snapshotId, item.financialSnapshotId());
        assertEquals("v1", item.financialSnapshotVersion());
        assertEquals("DEBTOR-001", item.debtorAccountReference());
        assertEquals("TREASURY-001", item.creditorAccountReference());
        assertEquals(2, item.frozenEntries().size());
        assertEquals("DEBIT", item.frozenEntries().get(0).direction());
        assertEquals("CREDIT", item.frozenEntries().get(1).direction());
        assertNotSame(candidate.frozenEntries(), item.frozenEntries());
    }
}
