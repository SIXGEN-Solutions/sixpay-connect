package com.sixpay.accounting.architecture;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AccountingPersistenceArchitectureTest {

    private static final Path ROOT = Path.of(
            "src/main/java/com/sixpay/accounting"
    );

    @Test
    void paymentAssignmentHasDatabaseUniquenessGuard()
            throws Exception {
        String source = Files.readString(
                ROOT.resolve(
                        "infrastructure/persistence/"
                                + "AccountingBatchItemJpaEntity.java"
                )
        );

        assertTrue(
                source.contains(
                        "uk_accounting_batch_items_payment_id"
                )
        );
        assertTrue(
                source.contains(
                        "columnNames = \"payment_id\""
                )
        );
    }

    @Test
    void batchIdempotencyHasDatabaseUniquenessGuard()
            throws Exception {
        String source = Files.readString(
                ROOT.resolve(
                        "infrastructure/persistence/"
                                + "AccountingBatchJpaEntity.java"
                )
        );

        assertTrue(
                source.contains(
                        "uk_accounting_batches_idempotency_key"
                )
        );
        assertTrue(
                source.contains(
                        "columnNames = \"idempotency_key\""
                )
        );
    }

    @Test
    void jpaStaysInInfrastructure()
            throws Exception {
        Path domain = ROOT.resolve("domain");
        Path application = ROOT.resolve(
                "application"
        );

        assertNoJpa(domain);
        assertNoJpa(application);
    }

    private static void assertNoJpa(
            Path root
    ) throws Exception {
        try (var paths = Files.walk(root)) {
            var violations = paths
                    .filter(Files::isRegularFile)
                    .filter(path ->
                            path.toString().endsWith(".java")
                    )
                    .filter(path -> {
                        try {
                            return Files.readString(path)
                                    .contains(
                                            "jakarta.persistence."
                                    );
                        } catch (Exception exception) {
                            throw new IllegalStateException(
                                    exception
                            );
                        }
                    })
                    .toList();

            assertTrue(
                    violations.isEmpty(),
                    () -> "JPA leaked outside infrastructure: "
                            + violations
            );
        }
    }
}