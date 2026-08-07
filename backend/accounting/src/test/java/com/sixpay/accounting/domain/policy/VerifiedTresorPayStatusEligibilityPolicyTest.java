package com.sixpay.accounting.domain.policy;

import com.sixpay.accounting.domain.model.AccountingPaymentCandidate;
import com.sixpay.accounting.domain.model.TresorPayPaymentStatusEvidence;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Currency;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VerifiedTresorPayStatusEligibilityPolicyTest {

    private final VerifiedTresorPayStatusEligibilityPolicy policy =
            new VerifiedTresorPayStatusEligibilityPolicy();

    @Test
    void acceptsPaymentInsideWindowWithTresorPayStatusEvidence() {
        AccountingSelectionWindow window =
                new AccountingSelectionWindow(
                        LocalDate.of(2026, 8, 7),
                        Instant.parse("2026-08-06T22:00:00Z"),
                        Instant.parse("2026-08-07T22:00:00Z")
                );

        assertTrue(
                policy.evaluate(
                        candidate(
                                Instant.parse("2026-08-07T12:00:00Z"),
                                Instant.parse("2026-08-07T12:05:00Z")
                        ),
                        window
                ).eligible()
        );
    }

    @Test
    void rejectsStatusCheckPerformedAfterCutoff() {
        AccountingSelectionWindow window =
                new AccountingSelectionWindow(
                        LocalDate.of(2026, 8, 7),
                        Instant.parse("2026-08-06T22:00:00Z"),
                        Instant.parse("2026-08-07T22:00:00Z")
                );

        assertFalse(
                policy.evaluate(
                        candidate(
                                Instant.parse("2026-08-07T12:00:00Z"),
                                Instant.parse("2026-08-07T22:00:01Z")
                        ),
                        window
                ).eligible()
        );
    }

    private static AccountingPaymentCandidate candidate(
            Instant occurredAt,
            Instant checkedAt
    ) {
        return new AccountingPaymentCandidate(
                UUID.fromString(
                        "7ed75090-8af7-4dfa-9b62-8e4dca73501a"
                ),
                "PAY-20260807-0001",
                "TRESORPAY",
                "LAREGIONALE",
                new BigDecimal("10000"),
                Currency.getInstance("XAF"),
                occurredAt,
                LocalDate.of(2026, 8, 7),
                "AMP-POST-0001",
                new TresorPayPaymentStatusEvidence(
                        "CONFIRMED",
                        checkedAt,
                        "STATUS-REQ-0001",
                        "corr-accounting-1"
                )
        );
    }
}
