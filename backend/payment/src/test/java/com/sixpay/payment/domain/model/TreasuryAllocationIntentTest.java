package com.sixpay.payment.domain.model;

import com.sixpay.sharedkernel.domain.valueobject.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TreasuryAllocationIntentTest {

    @Test
    void allocationsAreCanonicalizedAndSumExactlyToTheTotal() {
        TreasuryAllocation second = allocation(
                "TAX_B",
                "60.00",
                "XAF"
        );
        TreasuryAllocation first = allocation(
                "TAX_A",
                "40.00",
                "XAF"
        );

        TreasuryAllocationIntent intent =
                new TreasuryAllocationIntent(
                        List.of(second, first),
                        Money.of(
                                new BigDecimal("100.00"),
                                "XAF"
                        )
                );

        assertEquals(
                List.of(first, second),
                intent.allocations()
        );
        assertEquals(
                Money.of(new BigDecimal("100"), "XAF"),
                intent.totalAmount()
        );
        assertEquals(
                "XAF",
                intent.currency().getCurrencyCode()
        );
    }

    @Test
    void allocationsAreDefensivelyCopied() {
        List<TreasuryAllocation> mutable =
                new ArrayList<>();
        mutable.add(
                allocation("TAX_A", "100", "XAF")
        );

        TreasuryAllocationIntent intent =
                new TreasuryAllocationIntent(
                        mutable,
                        Money.of(new BigDecimal("100"), "XAF")
                );

        mutable.clear();

        assertEquals(1, intent.allocations().size());
        assertThrows(
                UnsupportedOperationException.class,
                () -> intent.allocations().clear()
        );
    }

    @Test
    void duplicateBeneficiariesAreRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new TreasuryAllocationIntent(
                        List.of(
                                allocation("TAX_A", "40", "XAF"),
                                allocation("TAX_A", "60", "XAF")
                        ),
                        Money.of(new BigDecimal("100"), "XAF")
                )
        );
    }

    @Test
    void currencyMismatchAndIncorrectSumAreRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new TreasuryAllocationIntent(
                        List.of(
                                allocation("TAX_A", "100", "USD")
                        ),
                        Money.of(new BigDecimal("100"), "XAF")
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new TreasuryAllocationIntent(
                        List.of(
                                allocation("TAX_A", "99", "XAF")
                        ),
                        Money.of(new BigDecimal("100"), "XAF")
                )
        );
    }

    @Test
    void intentIsBoundedToTwentyPositiveAllocations() {
        List<TreasuryAllocation> allocations =
                new ArrayList<>();

        for (int index = 0; index < 21; index++) {
            allocations.add(
                    allocation(
                            "BENEFICIARY_" + index,
                            "1",
                            "XAF"
                    )
            );
        }

        assertThrows(
                IllegalArgumentException.class,
                () -> new TreasuryAllocationIntent(
                        allocations,
                        Money.of(new BigDecimal("21"), "XAF")
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> allocation("TAX_A", "0", "XAF")
        );

        assertTrue(
                TreasuryBeneficiaryReference.of("A")
                        .compareTo(
                                TreasuryBeneficiaryReference.of("B")
                        ) < 0
        );
    }

    private static TreasuryAllocation allocation(
            String beneficiary,
            String amount,
            String currency
    ) {
        return new TreasuryAllocation(
                TreasuryBeneficiaryReference.of(beneficiary),
                Money.of(new BigDecimal(amount), currency)
        );
    }
}
