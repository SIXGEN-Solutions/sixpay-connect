package com.sixpay.accounting.domain.policy;

import com.sixpay.accounting.domain.model.AccountingPaymentCandidate;
import com.sixpay.accounting.domain.model.TresorPayPaymentStatusEvidence;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VerifiedTresorPayStatusEligibilityPolicyTest {

    private final VerifiedTresorPayStatusEligibilityPolicy policy =
            new VerifiedTresorPayStatusEligibilityPolicy();

    @Test
    void completedTresorPayStatusIsEligible() {
        AccountingSelectionWindow window = new AccountingSelectionWindow(
                LocalDate.of(2026, 8, 11),
                Instant.parse("2026-08-10T18:00:00Z"),
                Instant.parse("2026-08-11T18:00:00Z")
        );

        assertTrue(policy.evaluate(
                candidate("COMPLETED", "2026-08-11T11:59:00Z"),
                window
        ).eligible());
    }

    @Test
    void nonCompletedTresorPayStatusIsNotEligible() {
        AccountingSelectionWindow window = new AccountingSelectionWindow(
                LocalDate.of(2026, 8, 11),
                Instant.parse("2026-08-10T18:00:00Z"),
                Instant.parse("2026-08-11T18:00:00Z")
        );

        assertFalse(policy.evaluate(
                candidate("PENDING", "2026-08-11T11:59:00Z"),
                window
        ).eligible());
    }

    private static AccountingPaymentCandidate candidate(
            String status,
            String checkedAt
    ) {
        return new AccountingPaymentCandidate(
                UUID.randomUUID(),
                "REF-DGI-2026-0042",
                "partner",
                "LRB",
                new BigDecimal("1000.00"),
                Currency.getInstance("XAF"),
                Instant.parse("2026-08-11T11:58:00Z"),
                LocalDate.of(2026, 8, 11),
                "RB-2026081100045",
                UUID.fromString("5b72d5b1-2d1e-4f18-a8e0-1c8e19d31d01"),
                "v1",
                Instant.parse("2026-08-11T11:58:40Z"),
                "DEBTOR-ACCOUNT-REF",
                "TREASURY-ACCOUNT-REF",
                List.of(
                        new AccountingPaymentCandidate.FrozenEntry(
                                UUID.fromString("e011c0a1-b3b9-461c-a7ca-899c97cbf101"),
                                1,
                                "DEBIT",
                                "DEBTOR-ACCOUNT-REF",
                                new BigDecimal("1000.00"),
                                Currency.getInstance("XAF"),
                                Instant.parse("2026-08-11T11:58:30Z")
                        ),
                        new AccountingPaymentCandidate.FrozenEntry(
                                UUID.fromString("e011c0a1-b3b9-461c-a7ca-899c97cbf102"),
                                2,
                                "CREDIT",
                                "TREASURY-ACCOUNT-REF",
                                new BigDecimal("1000.00"),
                                Currency.getInstance("XAF"),
                                Instant.parse("2026-08-11T11:58:31Z")
                        )
                ),
                new TresorPayPaymentStatusEvidence(
                        "REF-DGI-2026-0042",
                        "EXT_TRESORPAY-CM_D46J080300003",
                        status,
                        "BANK_TRANSFER",
                        "RB-2026081100045",
                        true,
                        true,
                        Instant.parse("2026-08-11T11:58:20Z"),
                        null,
                        Instant.parse(checkedAt),
                        "REF-DGI-2026-0042",
                        "corr-1"
                )
        );
    }
}
