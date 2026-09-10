package com.sixpay.payment.domain.model.financial;

import com.sixpay.sharedkernel.domain.valueobject.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;

class PaymentFinancialEntrySnapshotTest {

    @Test
    void refusesNonPositiveSequence() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new PaymentFinancialEntrySnapshot(
                        UUID.fromString(
                                "11111111-1111-4111-8111-111111111111"
                        ),
                        0,
                        FinancialEntryDirection.DEBIT,
                        "debtor-ref",
                        Money.of(
                                new BigDecimal("1000"),
                                "XAF"
                        ),
                        Instant.parse(
                                "2026-09-06T18:00:00Z"
                        )
                )
        );
    }

    @Test
    void refusesNonPositiveAmount() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new PaymentFinancialEntrySnapshot(
                        UUID.fromString(
                                "11111111-1111-4111-8111-111111111111"
                        ),
                        1,
                        FinancialEntryDirection.DEBIT,
                        "debtor-ref",
                        Money.zero(
                                Currency.getInstance("XAF")
                        ),
                        Instant.parse(
                                "2026-09-06T18:00:00Z"
                        )
                )
        );
    }
}
