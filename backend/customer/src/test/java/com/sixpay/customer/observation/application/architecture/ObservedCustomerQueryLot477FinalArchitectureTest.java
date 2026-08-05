package com.sixpay.customer.observation.application.architecture;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ObservedCustomerQueryLot477FinalArchitectureTest {

    private static final Path ROOT = Path.of(
            "src/main/java/com/sixpay/customer/observation"
    );

    @Test
    void customerObservationHasTheApprovedLayeredStructure() {
        for (String directory : List.of(
                "api/controller",
                "api/dto",
                "api/error",
                "api/mapper",
                "application/port/input/query",
                "application/port/output/query",
                "application/query",
                "application/service/query",
                "domain",
                "infrastructure/persistence",
                "infrastructure/query/adapter",
                "infrastructure/query/cursor",
                "infrastructure/query/mapper",
                "infrastructure/query/model",
                "configuration"
        )) {
            assertTrue(
                    Files.isDirectory(ROOT.resolve(directory)),
                    () -> "Missing required directory: " + directory
            );
        }
    }

    @Test
    void customerNeverDependsOnPaymentOrAmplitude()
            throws Exception {
        assertNoTokens(
                ROOT,
                List.of(
                        "import com.sixpay.payment.",
                        "PaymentOutboxEntity",
                        "Amplitude",
                        "amplitude"
                )
        );
    }

    @Test
    void applicationAndDomainRemainFreeOfApiSpringAndJpa()
            throws Exception {
        for (Path layer : List.of(
                ROOT.resolve("application"),
                ROOT.resolve("domain")
        )) {
            assertNoTokens(
                    layer,
                    List.of(
                            "import org.springframework.",
                            "import jakarta.persistence.",
                            "import org.hibernate.",
                            "import com.sixpay.customer.observation.api.",
                            "@RestController",
                            "@Entity",
                            "EntityManager",
                            "JdbcTemplate"
                    )
            );
        }
    }

    @Test
    void apiDoesNotDependOnSpringDataRepositories()
            throws Exception {
        assertNoTokens(
                ROOT.resolve("api"),
                List.of(
                        "SpringDataRepository",
                        "JpaRepository",
                        "CrudRepository",
                        "ObservedCustomerSpringDataRepository",
                        "ObservedPaymentSpringDataRepository",
                        "EntityManager",
                        "JdbcTemplate"
                )
        );
    }

    @Test
    void portsContainNoJpaOrApiDtos()
            throws Exception {
        assertNoTokens(
                ROOT.resolve("application/port"),
                List.of(
                        "jakarta.persistence",
                        "org.hibernate",
                        "EntityManager",
                        "JpaRepository",
                        "com.sixpay.customer.observation.api.dto"
                )
        );
    }

    @Test
    void onlyCustomerExposesObservedCustomerRestRoute()
            throws Exception {

        String controller = Files.readString(
                ROOT.resolve(
                        "api/controller/"
                                + "ObservedCustomerQueryController.java"
                )
        );

        assertTrue(
                controller.contains(
                        "/internal/api/v1/observed-customers"
                )
        );

        Path backendRoot = Path.of("..")
                .toAbsolutePath()
                .normalize();

        Path customerModuleRoot = Path.of(".")
                .toAbsolutePath()
                .normalize();

        if (!Files.isDirectory(backendRoot)) {
            return;
        }

        try (var paths = Files.walk(backendRoot)) {
            List<Path> offenders = paths
                    .filter(Files::isRegularFile)
                    .filter(path ->
                            path.toString().endsWith(".java")
                    )
                    /*
                     * Ignore the complete Customer module, including:
                     * - src/main
                     * - src/test
                     * - generated sources if present
                     *
                     * The purpose is to detect another backend module
                     * exposing the same route.
                     */
                    .filter(path ->
                            !path.toAbsolutePath()
                                    .normalize()
                                    .startsWith(customerModuleRoot)
                    )
                    .filter(path -> {
                        try {
                            return Files.readString(path)
                                    .contains(
                                            "/internal/api/v1/"
                                                    + "observed-customers"
                                    );
                        } catch (Exception exception) {
                            throw new IllegalStateException(
                                    "Cannot inspect " + path,
                                    exception
                            );
                        }
                    })
                    .toList();

            assertTrue(
                    offenders.isEmpty(),
                    () -> "Observed Customer API exposed outside "
                            + "Customer: "
                            + offenders
            );
        }
    }

    private static void assertNoTokens(
            Path root,
            List<String> forbidden
    ) throws Exception {
        try (var paths = Files.walk(root)) {
            List<String> violations = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .flatMap(path -> {
                        try {
                            String source = Files.readString(path);
                            return forbidden.stream()
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
                    () -> "Architecture violations: " + violations
            );
        }
    }
}
