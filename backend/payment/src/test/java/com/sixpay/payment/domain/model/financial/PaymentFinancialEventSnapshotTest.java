package com.sixpay.payment.domain.model.financial;

import com.sixpay.payment.domain.model.FinancialInstitutionCode;
import com.sixpay.payment.domain.model.PaymentId;
import com.sixpay.payment.domain.model.PublicPaymentReference;
import com.sixpay.sharedkernel.domain.valueobject.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PaymentFinancialEventSnapshotTest {

    private static final Instant T0 =
            Instant.parse("2026-09-06T18:00:00Z");

    @Test
    void finalizesAndBecomesImmutable() {
        PaymentFinancialEventSnapshot snapshot =
                draft();

        snapshot.addEntry(
                entry(
                        "11111111-1111-4111-8111-111111111111",
                        1,
                        FinancialEntryDirection.DEBIT,
                        "debtor-ref"
                )
        );
        snapshot.addEntry(
                entry(
                        "22222222-2222-4222-8222-222222222222",
                        2,
                        FinancialEntryDirection.CREDIT,
                        "creditor-ref"
                )
        );

        snapshot.finalizeAt(
                T0.plusSeconds(1)
        );

        assertEquals(
                FinancialSnapshotStatus.FINALIZED,
                snapshot.status()
        );
        assertEquals(
                2,
                snapshot.entries().size()
        );

        assertThrows(
                IllegalStateException.class,
                () -> snapshot.addEntry(
                        entry(
                                "33333333-3333-4333-8333-333333333333",
                                3,
                                FinancialEntryDirection.CREDIT,
                                "other-ref"
                        )
                )
        );
        assertThrows(
                IllegalStateException.class,
                () -> snapshot.finalizeAt(
                        T0.plusSeconds(2)
                )
        );
    }

    @Test
    void refusesFinalizationWithoutEntries() {
        PaymentFinancialEventSnapshot snapshot =
                draft();

        assertThrows(
                IllegalStateException.class,
                () -> snapshot.finalizeAt(
                        T0.plusSeconds(1)
                )
        );
    }

    @Test
    void refusesDuplicateEntrySequence() {
        PaymentFinancialEventSnapshot snapshot =
                draft();

        snapshot.addEntry(
                entry(
                        "11111111-1111-4111-8111-111111111111",
                        1,
                        FinancialEntryDirection.DEBIT,
                        "debtor-ref"
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> snapshot.addEntry(
                        entry(
                                "22222222-2222-4222-8222-222222222222",
                                1,
                                FinancialEntryDirection.CREDIT,
                                "creditor-ref"
                        )
                )
        );
    }

    private static PaymentFinancialEventSnapshot draft() {
        return PaymentFinancialEventSnapshot.draft(
                UUID.fromString(
                        "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa"
                ),
                new PaymentId(
                        UUID.fromString(
                                "bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb"
                        )
                ),
                PublicPaymentReference.of(
                        "PAY-01ARZ3NDEKTSV4RRFFQ69G5FAV"
                ),
                FinancialInstitutionCode.of("LRB"),
                "debtor-ref",
                "creditor-ref",
                Money.of(
                        new BigDecimal("1000.00"),
                        "XAF"
                ),
                "v1",
                T0
        );
    }

    private static PaymentFinancialEntrySnapshot entry(
            String id,
            int sequence,
            FinancialEntryDirection direction,
            String accountReference
    ) {
        return new PaymentFinancialEntrySnapshot(
                UUID.fromString(id),
                sequence,
                direction,
                accountReference,
                Money.of(
                        new BigDecimal("1000.00"),
                        "XAF"
                ),
                T0
        );
    }
}
