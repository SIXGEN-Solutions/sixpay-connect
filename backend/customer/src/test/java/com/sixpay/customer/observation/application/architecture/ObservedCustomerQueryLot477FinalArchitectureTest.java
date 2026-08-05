package com.sixpay.customer.observation.application.architecture;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ObservedCustomerQueryLot477FinalArchitectureTest {

    private static final Path ROOT = Path.of(
            "src/main/java/com/sixpay/customer/observation"
    );

    private static final String OBSERVED_CUSTOMER_ROUTE =
            "/internal/api/v1/observed-customers";

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
            Path expected =
                    ROOT.resolve(directory);

            assertTrue(
                    Files.isDirectory(expected),
                    () -> "Missing required directory: "
                            + expected.toAbsolutePath()
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

        Path customerController =
                ROOT.resolve(
                        "api/controller/"
                                + "ObservedCustomerQueryController.java"
                );

        String controllerSource =
                Files.readString(customerController);

        assertTrue(
                controllerSource.contains("@RestController"),
                "Observed Customer controller must be a REST controller"
        );

        assertTrue(
                controllerSource.contains(
                        "@RequestMapping("
                                + "\""
                                + OBSERVED_CUSTOMER_ROUTE
                                + "\""
                                + ")"
                ),
                "Observed Customer controller must expose the canonical route"
        );

        Path customerModuleRoot =
                Path.of("")
                        .toAbsolutePath()
                        .normalize();

        Path backendRoot =
                customerModuleRoot
                        .getParent();

        if (backendRoot == null
                || !Files.isDirectory(backendRoot)) {
            return;
        }

        try (var paths = Files.walk(backendRoot)) {
            List<Path> offenders = paths
                    .filter(Files::isRegularFile)
                    .filter(path ->
                            path.toString().endsWith(".java")
                    )
                    /*
                     * Customer itself is the owner of the REST API.
                     * Its production and test sources are excluded.
                     */
                    .filter(path ->
                            !path.toAbsolutePath()
                                    .normalize()
                                    .startsWith(customerModuleRoot)
                    )
                    .filter(path -> {
                        try {
                            String source =
                                    Files.readString(path);

                            return exposesRestRoute(
                                    source,
                                    OBSERVED_CUSTOMER_ROUTE
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
                    () -> "Observed Customer REST API exposed "
                            + "outside Customer: "
                            + offenders
            );
        }
    }

    private static boolean exposesRestRoute(
            String source,
            String route
    ) {
        /*
         * A simple occurrence in OpenAPI pathsToMatch, tests or
         * documentation is not an endpoint exposure.
         *
         * Only Spring MVC controller classes are considered.
         */
        boolean isController =
                source.contains("@RestController")
                        || source.contains("@Controller");

        if (!isController) {
            return false;
        }

        boolean containsRoute =
                source.contains(route);

        if (!containsRoute) {
            return false;
        }

        return source.contains("@RequestMapping")
                || source.contains("@GetMapping")
                || source.contains("@PostMapping")
                || source.contains("@PutMapping")
                || source.contains("@PatchMapping")
                || source.contains("@DeleteMapping");
    }

    private static void assertNoTokens(
            Path root,
            List<String> forbidden
    ) throws Exception {

        assertTrue(
                Files.isDirectory(root),
                () -> "Missing source directory: "
                        + root.toAbsolutePath()
        );

        try (var paths = Files.walk(root)) {
            List<String> violations = paths
                    .filter(Files::isRegularFile)
                    .filter(path ->
                            path.toString().endsWith(".java")
                    )
                    .flatMap(path -> {
                        try {
                            String source =
                                    Files.readString(path);

                            return forbidden.stream()
                                    .filter(source::contains)
                                    .map(token ->
                                            path
                                                    + " contains "
                                                    + token
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
                    violations.isEmpty(),
                    () -> "Architecture violations: "
                            + violations
            );
        }
    }
}