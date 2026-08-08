package com.sixpay.reporting.architecture;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportingFoundationArchitectureTest {

    private static final Path REPORTING = Path.of(
            "src/main/java/com/sixpay/reporting"
    );

    private static final Path REPOSITORY_ROOT =
            Path.of("../..").normalize();

    @Test
    void reportingContainsGoldenModuleFoundation() {

        for (String relative : List.of(
                "api/controller",
                "api/dto",
                "api/mapper",
                "api/exception",
                "application/port/input",
                "application/port/output",
                "application/query",
                "application/service",
                "configuration",
                "domain/model",
                "domain/policy",
                "domain/exception",
                "events",
                "infrastructure/persistence",
                "infrastructure/query",
                "infrastructure/export"
        )) {
            Path expected = REPORTING.resolve(relative);

            assertTrue(
                    Files.isDirectory(expected),
                    () -> "Missing Reporting foundation: "
                            + expected.toAbsolutePath()
            );
        }
    }

    @Test
    void reportingIsAStandardNonExecutableJar()
            throws Exception {

        String pom = Files.readString(Path.of("pom.xml"));

        assertTrue(pom.contains("<packaging>jar</packaging>"));
        assertFalse(pom.contains("spring-boot-maven-plugin"));
        assertFalse(pom.contains("<artifactId>bootstrap</artifactId>"));
    }

    @Test
    void bootstrapComposesReporting()
            throws Exception {

        String bootstrapPom = Files.readString(
                REPOSITORY_ROOT.resolve(
                        "backend/bootstrap/pom.xml"
                )
        );

        assertTrue(
                bootstrapPom.contains(
                        "<artifactId>reporting</artifactId>"
                )
        );
    }

    @Test
    void reportingApplicationAndDomainRemainFrameworkFree()
            throws Exception {

        for (Path layer : List.of(
                REPORTING.resolve("application"),
                REPORTING.resolve("domain")
        )) {
            assertNoTokens(
                    layer,
                    List.of(
                            "import org.springframework.",
                            "import jakarta.persistence.",
                            "import org.hibernate.",
                            "EntityManager",
                            "JdbcTemplate",
                            "@Entity",
                            "@Repository",
                            "@Service",
                            "@Component",
                            "@Transactional",
                            "@RestController"
                    )
            );
        }
    }

    @Test
    void reportingDoesNotDependOnOtherBusinessAggregates()
            throws Exception {

        assertNoTokens(
                REPORTING,
                List.of(
                        "import com.sixpay.payment.",
                        "import com.sixpay.customer.",
                        "import com.sixpay.accounting.",
                        "import com.sixpay.notification.",
                        "PaymentRepository",
                        "ObservedCustomerRepository"
                )
        );
    }

    @Test
    void reportingExposesOnlyImplementedPhase6AuditEndpoints()
            throws Exception {

        String controller = Files.readString(
                REPORTING.resolve(
                        "api/controller/"
                                + "PaymentAuditQueryController.java"
                )
        );

        assertTrue(
                controller.contains(
                        "/internal/api/v1/payment-audit-records"
                )
        );

        assertTrue(
                controller.contains(
                        "/internal/api/v1/payment-audit-records/{auditId}"
                )
        );

        assertTrue(
                controller.contains(
                        "/internal/api/v1/payments/{paymentId}/timeline"
                )
        );

        /*
         * Export belongs to Lot 6.5 and must still be absent.
         */
        assertFalse(
                controller.contains(
                        "/internal/api/v1/payment-audit-exports"
                )
        );

        assertFalse(controller.contains("@PostMapping"));
        assertFalse(controller.contains("@PutMapping"));
        assertFalse(controller.contains("@DeleteMapping"));
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
                            throw new IllegalStateException(
                                    "Cannot inspect " + path,
                                    exception
                            );
                        }
                    })
                    .toList();

            assertTrue(
                    violations.isEmpty(),
                    () -> "Reporting architecture violations: "
                            + violations
            );
        }
    }
}
