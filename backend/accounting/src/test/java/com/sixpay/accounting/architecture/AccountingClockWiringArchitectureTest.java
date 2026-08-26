package com.sixpay.accounting.architecture;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AccountingClockWiringArchitectureTest {

    private static final Path CONFIGURATION =
            Path.of(
                    "src/main/java/com/sixpay/accounting/"
                            + "configuration/"
                            + "AccountingModuleConfiguration.java"
            );

    @Test
    void accountingClockIsModuleScopedByName()
            throws Exception {

        String source =
                Files.readString(CONFIGURATION);

        assertTrue(
                source.contains(
                        "ACCOUNTING_CLOCK"
                )
        );

        assertTrue(
                source.contains(
                        "@Bean(name = ACCOUNTING_CLOCK)"
                )
        );

        assertTrue(
                source.contains(
                        "name = ACCOUNTING_CLOCK"
                )
        );
    }

    @Test
    void accountingClockConsumersAreQualified()
            throws Exception {

        String source =
                Files.readString(CONFIGURATION);

        int first =
                source.indexOf(
                        "@Qualifier(ACCOUNTING_CLOCK)"
                );

        int second =
                source.indexOf(
                        "@Qualifier(ACCOUNTING_CLOCK)",
                        first + 1
                );

        assertTrue(
                first >= 0,
                "AccountingBatchBuilder must use accountingClock"
        );

        assertTrue(
                second > first,
                "AccountingBatchReconciliationService must "
                        + "use accountingClock"
        );
    }
}
