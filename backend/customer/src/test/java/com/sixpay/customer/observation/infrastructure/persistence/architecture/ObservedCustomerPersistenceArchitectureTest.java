package com.sixpay.customer.observation.infrastructure.persistence.architecture;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ObservedCustomerPersistenceArchitectureTest {

    private static final Path ROOT = Path.of(
            "src/main/java/com/sixpay/customer/observation/"
                    + "infrastructure/persistence"
    );

    @Test
    void jpaRemainsInsidePersistenceInfrastructure()
            throws Exception {

        assertTrue(Files.isDirectory(ROOT));

        try (var paths = Files.walk(ROOT)) {
            var violations = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .flatMap(path -> {
                        try {
                            String source = Files.readString(path);

                            return List.of(
                                            "import com.sixpay.payment.",
                                            "import com.sixpay.customer.verification.",
                                            "RestClient",
                                            "WebClient",
                                            "HttpClient",
                                            "AmplitudeCustomerVerification",
                                            "PaymentDomainEvent"
                                    )
                                    .stream()
                                    .filter(source::contains)
                                    .map(token ->
                                            path + " contains " + token
                                    );
                        } catch (Exception exception) {
                            throw new IllegalStateException(exception);
                        }
                    })
                    .toList();

            assertTrue(
                    violations.isEmpty(),
                    () -> "Persistence boundary violations: "
                            + violations
            );
        }
    }

    @Test
    void mainEntityUsesOptimisticLocking()
            throws Exception {

        String source = Files.readString(
                ROOT.resolve(
                        "entity/ObservedCustomerJpaEntity.java"
                )
        );

        assertTrue(source.contains("@Version"));
        assertTrue(source.contains("long rowVersion"));
        assertTrue(source.contains(
                "name = \"niu_search_hash\""
        ));
        assertTrue(source.contains(
                "name = \"niu_protected\""
        ));
        assertTrue(source.contains(
                "name = \"legal_name_protected\""
        ));
    }

    @Test
    void transactionDecoratorRetriesBoundedConflicts()
            throws Exception {

        String source = Files.readString(
                ROOT.resolve(
                        "transaction/"
                                + "TransactionalObserveCustomerUseCase.java"
                )
        );

        assertTrue(source.contains(
                "ObjectOptimisticLockingFailureException"
        ));
        assertTrue(source.contains(
                "DataIntegrityViolationException"
        ));
        assertTrue(source.contains(
                "attempt <= maxAttempts"
        ));
        assertFalse(source.contains("Thread.sleep("));
        assertFalse(source.contains("Instant.now("));
    }

    @Test
    void persistenceNeverStoresRawAccountOrPlainNiuColumns()
            throws Exception {

        String migration = Files.readString(
                Path.of(
                        "src/main/resources/db/migration/"
                                + "V20260803_01__create_"
                                + "customer_observed_projection.sql"
                )
        );

        for (String forbidden : List.of(
                " account_number ",
                " rib_debiteur ",
                " raw_account ",
                " niu VARCHAR",
                " legal_name VARCHAR"
        )) {
            assertFalse(
                    migration.toLowerCase()
                            .contains(forbidden.strip()
                                    .toLowerCase()),
                    () -> "Unsafe column found: " + forbidden
            );
        }

        assertTrue(migration.contains("niu_protected"));
        assertTrue(migration.contains("niu_search_hash"));
        assertTrue(migration.contains(
                "account_binding_fingerprint"
        ));
        assertTrue(migration.contains("masked_value"));
        assertTrue(migration.contains(
                "UNIQUE (niu_search_hash)"
        ));
        assertTrue(migration.contains(
                "source_event_id UUID PRIMARY KEY"
        ));
    }
}
