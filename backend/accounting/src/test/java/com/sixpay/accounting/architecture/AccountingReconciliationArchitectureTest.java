package com.sixpay.accounting.architecture;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccountingReconciliationArchitectureTest {

    private static final Path SERVICE = Path.of(
            "src/main/java/com/sixpay/accounting/"
                    + "application/service/"
                    + "AccountingBatchReconciliationService.java"
    );

    @Test
    void unknownOutcomeIsResolvedByLookupNotResubmission()
            throws Exception {
        String source = Files.readString(SERVICE);

        assertTrue(
                source.contains(
                        "AccountingSubmissionOutcomeUnknownException"
                )
        );
        assertTrue(
                source.contains(
                        "findByIdempotencyKey("
                )
        );

        for (String forbidden : List.of(
                "RetryTemplate",
                "@Retryable",
                "while (",
                "for (int attempt"
        )) {
            assertFalse(
                    source.contains(forbidden),
                    () -> "Forbidden retry mechanism: "
                            + forbidden
            );
        }
    }

    @Test
    void providerTrackingDoesNotPolluteBatchBusinessStatus()
            throws Exception {
        String batchStatus = Files.readString(
                Path.of(
                        "src/main/java/com/sixpay/accounting/"
                                + "domain/model/"
                                + "AccountingBatchStatus.java"
                )
        );

        assertFalse(
                batchStatus.contains(
                        "OUTCOME_UNKNOWN"
                )
        );
        assertFalse(
                batchStatus.contains(
                        "SUBMITTED"
                )
        );

        String submissionState = Files.readString(
                Path.of(
                        "src/main/java/com/sixpay/accounting/"
                                + "domain/model/"
                                + "AccountingSubmissionState.java"
                )
        );

        assertTrue(
                submissionState.contains(
                        "OUTCOME_UNKNOWN"
                )
        );
        assertTrue(
                submissionState.contains(
                        "RECONCILIATION_REQUIRED"
                )
        );
    }

    @Test
    void existingBatchItemsAreUpdatedInPlace()
            throws Exception {
        String entity = Files.readString(
                Path.of(
                        "src/main/java/com/sixpay/accounting/"
                                + "infrastructure/persistence/"
                                + "AccountingBatchJpaEntity.java"
                )
        );

        assertTrue(
                entity.contains(
                        "entity.synchronize(domainItem)"
                )
        );

        assertFalse(
                normalize(entity).contains(
                        "items.clear();"
                )
        );
    }

    private static String normalize(
            String source
    ) {
        return source
                .replace(" ", "")
                .replace("\\t", "")
                .replace("\\r", "")
                .replace("\\n", "");
    }
}
