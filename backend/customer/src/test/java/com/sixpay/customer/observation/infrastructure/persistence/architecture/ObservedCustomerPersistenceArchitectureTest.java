package com.sixpay.customer.observation.infrastructure
        .persistence.architecture;

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

    private static final Path CUSTOMER_BASELINE = Path.of(
            "src/main/resources/db/migration/"
                    + "V200__customer_baseline.sql"
    );

    private static final String OBSERVED_CUSTOMER_START =
            "CREATE TABLE customer_observed_customer";

    private static final String CUSTOMER_MANAGEMENT_START =
            "CREATE TABLE customer_management_customer";

    @Test
    void jpaRemainsInsidePersistenceInfrastructure()
            throws Exception {

        assertTrue(Files.isDirectory(ROOT));

        try (var paths = Files.walk(ROOT)) {
            var violations = paths
                    .filter(Files::isRegularFile)
                    .filter(path ->
                            path.toString().endsWith(".java")
                    )
                    .flatMap(path -> {
                        try {
                            String source =
                                    Files.readString(path);

                            return List.of(
                                            "import com.sixpay.payment.",
                                            "import com.sixpay.customer."
                                                    + "verification.",
                                            "RestClient",
                                            "WebClient",
                                            "HttpClient",
                                            "AmplitudeCustomerVerification",
                                            "PaymentDomainEvent"
                                    )
                                    .stream()
                                    .filter(source::contains)
                                    .map(token ->
                                            path
                                                    + " contains "
                                                    + token
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

        assertTrue(
                source.contains("@Version")
        );

        assertTrue(
                source.contains("long rowVersion")
        );

        assertTrue(
                source.contains(
                        "name = \"niu_search_hash\""
                )
        );

        assertTrue(
                source.contains(
                        "name = \"niu_protected\""
                )
        );

        assertTrue(
                source.contains(
                        "name = \"legal_name_protected\""
                )
        );
    }

    @Test
    void transactionDecoratorUsesExplicitBoundedRetryPolicy()
            throws Exception {

        String source = Files.readString(
                ROOT.resolve(
                        "transaction/"
                                + "TransactionalObserveCustomerUseCase.java"
                )
        );

        assertTrue(
                source.contains(
                        "ObservedCustomerProjectionFailureClassifier"
                ),
                "Transaction decorator must use the explicit "
                        + "failure classifier"
        );

        assertTrue(
                source.contains(
                        "ObservedCustomerProjectionRetryPolicy"
                ),
                "Transaction decorator must use the bounded "
                        + "retry policy"
        );

        assertTrue(
                source.contains(
                        "classifier.classify("
                ),
                "Failures must be classified before retry"
        );

        assertTrue(
                source.contains(
                        "retryPolicy.maxAttempts()"
                ),
                "Retry loop must use the configured maximum attempts"
        );

        assertTrue(
                source.contains(
                        "retryPolicy.shouldRetry("
                ),
                "Retry decision must be delegated to the policy"
        );

        assertTrue(
                source.contains(
                        "retryPolicy.beforeRetry("
                ),
                "Backoff must be delegated to the retry policy"
        );

        assertTrue(
                source.contains(
                        "ObservedCustomerProjectionRetryExhaustedException"
                ),
                "Retry exhaustion must be represented explicitly"
        );

        assertFalse(
                source.contains(
                        "ObjectOptimisticLockingFailureException"
                ),
                "Optimistic locking classification must not remain "
                        + "inside the transaction decorator"
        );

        assertFalse(
                source.contains(
                        "DataIntegrityViolationException"
                ),
                "Integrity failure classification must not remain "
                        + "inside the transaction decorator"
        );

        assertFalse(
                source.contains("Thread.sleep("),
                "Retry must not use Thread.sleep()"
        );

        assertFalse(
                source.contains("Instant.now("),
                "Retry must not use an uncontrolled system clock"
        );
    }

    @Test
    void persistenceNeverStoresRawAccountOrPlainNiuColumns()
            throws Exception {

        assertTrue(
                Files.isRegularFile(CUSTOMER_BASELINE),
                () -> "Customer baseline migration not found: "
                        + CUSTOMER_BASELINE
        );

        String baseline =
                Files.readString(CUSTOMER_BASELINE);

        String observedCustomerMigration =
                observedCustomerSection(baseline);

        for (String forbidden : List.of(
                "account_number",
                "rib_debiteur",
                "raw_account",
                "niu VARCHAR",
                "legal_name VARCHAR"
        )) {
            assertFalse(
                    observedCustomerMigration
                            .toLowerCase()
                            .contains(
                                    forbidden.toLowerCase()
                            ),
                    () -> "Unsafe Observed Customer column found: "
                            + forbidden
            );
        }

        assertTrue(
                observedCustomerMigration.contains(
                        "niu_protected"
                )
        );

        assertTrue(
                observedCustomerMigration.contains(
                        "niu_search_hash"
                )
        );

        assertTrue(
                observedCustomerMigration.contains(
                        "account_binding_fingerprint"
                )
        );

        assertTrue(
                observedCustomerMigration.contains(
                        "masked_value"
                )
        );

        assertTrue(
                observedCustomerMigration.contains(
                        "UNIQUE (niu_search_hash)"
                )
        );

        assertTrue(
                observedCustomerMigration.contains(
                        "source_event_id UUID PRIMARY KEY"
                )
        );
    }

    private String observedCustomerSection(
            String baseline
    ) {

        int start =
                baseline.indexOf(
                        OBSERVED_CUSTOMER_START
                );

        assertTrue(
                start >= 0,
                "Observed Customer section not found "
                        + "input canonical Customer baseline"
        );

        int customerManagementStart =
                baseline.indexOf(
                        CUSTOMER_MANAGEMENT_START,
                        start
                );

        assertTrue(
                customerManagementStart > start,
                "Customer Management section must appear "
                        + "after Observed Customer input V200"
        );

        return baseline.substring(
                start,
                customerManagementStart
        );
    }
}