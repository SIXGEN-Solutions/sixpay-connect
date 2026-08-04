package com.sixpay.customer.observation.architecture;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ObservedCustomerLot457ArchitectureTest {

    private static final Path CUSTOMER_ROOT = Path.of(
            "src/main/java/com/sixpay/customer"
    );

    @Test
    void customerNeverDependsOnPayment() throws Exception {
        try (var paths = Files.walk(CUSTOMER_ROOT)) {
            var violations = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .flatMap(path -> {
                        try {
                            String source = Files.readString(path);
                            return source.contains(
                                    "import com.sixpay.payment."
                            )
                                    ? java.util.stream.Stream.of(
                                            path + " imports Payment"
                                    )
                                    : java.util.stream.Stream.empty();
                        } catch (Exception exception) {
                            throw new IllegalStateException(exception);
                        }
                    })
                    .toList();

            assertTrue(
                    violations.isEmpty(),
                    () -> "Customer to Payment violations: "
                            + violations
            );
        }
    }

    @Test
    void observationDomainRemainsFrameworkFreeAndTimeExplicit()
            throws Exception {

        Path domain = CUSTOMER_ROOT.resolve(
                "observation/domain"
        );

        try (var paths = Files.walk(domain)) {
            var violations = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .flatMap(path -> {
                        try {
                            String source = Files.readString(path);
                            return List.of(
                                            "import org.springframework.",
                                            "import jakarta.persistence.",
                                            "import org.hibernate.",
                                            "Instant.now(",
                                            "System.currentTimeMillis(",
                                            "UUID.randomUUID("
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
                    () -> "Observation domain violations: "
                            + violations
            );
        }
    }

    @Test
    void paymentBoundaryAndBootstrapAreTheOnlyCrossModuleBridge()
            throws Exception {

        Path paymentPort = Path.of(
                "../payment/src/main/java/com/sixpay/payment/"
                        + "application/port/output/"
                        + "ObservedCustomerProjectionPort.java"
        );
        Path bootstrapAdapter = Path.of(
                "../bootstrap/src/main/java/com/sixpay/bootstrap/"
                        + "integration/customer/"
                        + "ObservedCustomerProjectionModuleAdapter.java"
        );

        assertTrue(Files.isRegularFile(paymentPort));
        assertTrue(Files.isRegularFile(bootstrapAdapter));

        String port = Files.readString(paymentPort);
        String adapter = Files.readString(bootstrapAdapter);

        assertFalse(port.contains(
                "import com.sixpay.customer."
        ));
        assertTrue(adapter.contains(
                "implements ObservedCustomerProjectionPort"
        ));
        assertTrue(adapter.contains(
                "ObserveCustomerUseCase"
        ));
    }

    @Test
    void migrationDefinesProjectionAndIdempotenceConstraints()
            throws Exception {

        String migration = Files.readString(
                Path.of(
                        "src/main/resources/db/migration/"
                                + "V20260803_01__create_"
                                + "customer_observed_projection.sql"
                )
        );

        for (String required : List.of(
                "CREATE TABLE customer_observed_customer",
                "CREATE TABLE customer_observed_institution",
                "CREATE TABLE customer_observed_account",
                "CREATE TABLE customer_observed_payment",
                "CREATE TABLE customer_observation_processed_event",
                "source_event_id UUID PRIMARY KEY",
                "niu_search_hash VARCHAR(64) NOT NULL",
                "row_version BIGINT NOT NULL"
        )) {
            assertTrue(
                    migration.contains(required),
                    () -> "Missing migration rule: " + required
            );
        }
    }
}
