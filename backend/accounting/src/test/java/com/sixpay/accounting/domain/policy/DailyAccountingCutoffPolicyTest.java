package com.sixpay.accounting.domain.policy;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DailyAccountingCutoffPolicyTest {

    private final DailyAccountingCutoffPolicy policy =
            new DailyAccountingCutoffPolicy(
                    ZoneId.of("Africa/Douala"),
                    LocalTime.of(23, 0)
            );

    @Test
    void autoModeUsesLatestClosedBusinessDate() {
        AccountingSelectionWindow window =
                policy.resolve(
                        Instant.parse("2026-08-07T10:00:00Z"),
                        AccountingCutoffMode.AUTO,
                        Optional.empty()
                );

        assertEquals(
                LocalDate.of(2026, 8, 6),
                window.businessDate()
        );
    }

    @Test
    void manualModeUsesRequestedBusinessDate() {
        AccountingSelectionWindow window =
                policy.resolve(
                        Instant.parse("2026-08-07T10:00:00Z"),
                        AccountingCutoffMode.MANUAL,
                        Optional.of(
                                LocalDate.of(2026, 8, 5)
                        )
                );

        assertEquals(
                LocalDate.of(2026, 8, 5),
                window.businessDate()
        );
    }

    @Test
    void manualModeRequiresDate() {
        assertThrows(
                IllegalArgumentException.class,
                () -> policy.resolve(
                        Instant.parse("2026-08-07T10:00:00Z"),
                        AccountingCutoffMode.MANUAL,
                        Optional.empty()
                )
        );
    }
}
