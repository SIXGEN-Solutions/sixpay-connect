package com.sixpay.payment.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentPerformanceArchitectureTest {

    private static final Path VIRTUAL_THREADS_TEST =
            Path.of(
                    "src/test/java/com/sixpay/payment/"
                            + "performance/"
                            + "PaymentVirtualThreadsTest.java"
            );

    private static final Path CONCURRENCY_TEST =
            Path.of(
                    "src/test/java/com/sixpay/payment/"
                            + "performance/"
                            + "PaymentConcurrencyPerformanceIT.java"
            );

    private static final Path ENTITY =
            Path.of(
                    "src/main/java/com/sixpay/payment/"
                            + "infrastructure/persistence/"
                            + "PaymentJpaEntity.java"
            );

    private static final Path COORDINATOR =
            Path.of(
                    "src/main/java/com/sixpay/payment/"
                            + "infrastructure/idempotency/"
                            + "PaymentIdempotencyConcurrencyCoordinator.java"
            );

    @Test
    void virtualThreadCapabilityIsTestedExplicitly()
            throws IOException {
        String source = Files.readString(
                VIRTUAL_THREADS_TEST
        );

        assertTrue(source.contains(
                "newVirtualThreadPerTaskExecutor"
        ));
        assertTrue(source.contains("isVirtual()"));
        assertTrue(source.contains("10_000"));
    }

    @Test
    void optimisticLockRemainsJpaVersionBased()
            throws IOException {
        String source = Files.readString(ENTITY);

        assertTrue(source.contains("@Version"));
        assertTrue(source.contains(
                "persistenceVersion"
        ));
    }

    @Test
    void strongConcurrencyUsesPostgresqlTransactionLock()
            throws IOException {
        String source = Files.readString(COORDINATOR);

        assertTrue(source.contains(
                "pg_advisory_xact_lock"
        ));
        assertTrue(source.contains(
                "Propagation.MANDATORY"
        ));
    }

    @Test
    void loadTestDoesNotIntroduceProductionBenchmarkCode()
            throws IOException {
        String source = Files.readString(
                CONCURRENCY_TEST
        );

        assertTrue(source.contains("3_000"));
        assertTrue(source.contains(
                "payment_idempotency"
        ));
        assertFalse(source.contains("Thread.sleep("));
    }
}
