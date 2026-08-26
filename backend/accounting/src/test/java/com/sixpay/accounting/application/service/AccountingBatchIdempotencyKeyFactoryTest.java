package com.sixpay.accounting.application.service;

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

class AccountingBatchIdempotencyKeyFactoryTest {

    private final AccountingBatchIdempotencyKeyFactory factory =
            new AccountingBatchIdempotencyKeyFactory();

    @Test
    void keyIsStableRegardlessOfCandidateOrder() {
        AccountingPaymentCandidate first = candidate(
                "7ed75090-8af7-4dfa-9b62-8e4dca73501a",
                "PAY-1"
        );
        AccountingPaymentCandidate second = candidate(
                "43d7e460-4ca7-4ed1-8603-9f11fb62dd65",
                "PAY-2"
        );

        var key1 = factory.create(
                "LAREGIONALE",
                LocalDate.of(2026, 8, 7),
                List.of(first, second)
        );

        var key2 = factory.create(
                "LAREGIONALE",
                LocalDate.of(2026, 8, 7),
                List.of(second, first)
        );

        assertEquals(key1, key2);
    }

    private static AccountingPaymentCandidate candidate(
            String paymentId,
            String reference
    ) {
        return new AccountingPaymentCandidate(
                UUID.fromString(paymentId),
                reference,
                "TRESORPAY",
                "LAREGIONALE",
                new BigDecimal("10000"),
                Currency.getInstance("XAF"),
                Instant.parse("2026-08-07T12:00:00Z"),
                LocalDate.of(2026, 8, 7),
                "AMP-" + reference,
                new TresorPayPaymentStatusEvidence(
                        "CONFIRMED",
                        Instant.parse("2026-08-07T12:05:00Z"),
                        "STATUS-" + reference,
                        "corr-" + reference
                )
        );
    }
}
