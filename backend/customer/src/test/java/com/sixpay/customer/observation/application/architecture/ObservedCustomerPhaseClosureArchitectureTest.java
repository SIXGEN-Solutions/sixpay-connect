package com.sixpay.customer.observation.application.architecture;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ObservedCustomerPhaseClosureArchitectureTest {

    private static final Path CUSTOMER = Path.of(
            "src/main/java/com/sixpay/customer/observation"
    );

    private static final Path REPOSITORY_ROOT = Path.of(
            "..",
            ".."
    ).normalize();

    @Test
    void phaseContainsItsRequiredProductionCapabilities() {

        for (String directory : List.of(
                "api/controller",
                "api/dto",
                "api/error",
                "api/mapper",
                "api/observability",
                "application/audit",
                "application/port/input",
                "application/port/output/audit",
                "application/port/output/query",
                "application/query",
                "application/service",
                "domain",
                "infrastructure/audit",
                "infrastructure/health",
                "infrastructure/observability",
                "infrastructure/persistence",
                "infrastructure/query",
                "infrastructure/resilience",
                "configuration"
        )) {
            Path expected = CUSTOMER.resolve(directory);

            assertTrue(
                    Files.isDirectory(expected),
                    () -> "Missing phase capability directory: "
                            + expected.toAbsolutePath()
            );
        }
    }

    @Test
    void phaseContainsFinalDocumentationAndAcceptanceScripts() {

        for (String relative : List.of(
                "documentation/implementation/customer-observation/"
                        + "README.md",
                "documentation/implementation/customer-observation/"
                        + "E2E-ACCEPTANCE-MATRIX.md",
                "documentation/implementation/customer-observation/"
                        + "OPERATIONS-RUNBOOK.md",
                "documentation/implementation/customer-observation/"
                        + "PHASE-CLOSURE-CHECKLIST.md",
                "scripts/validation/"
                        + "validate-customer-observation-phase.sh",
                "scripts/validation/"
                        + "validate-customer-observation-phase.ps1"
        )) {
            Path expected = REPOSITORY_ROOT.resolve(relative);

            assertTrue(
                    Files.isRegularFile(expected),
                    () -> "Missing phase closure asset: "
                            + expected.toAbsolutePath()
            );
        }
    }

    @Test
    void customerObservationRemainsIndependentFromPaymentAndAmplitude()
            throws Exception {

        assertNoTokens(
                CUSTOMER,
                List.of(
                        "import com.sixpay.payment.",
                        "PaymentOutboxEntity",
                        "Amplitude",
                        "amplitude"
                )
        );
    }

    @Test
    void applicationAndDomainRemainFrameworkFree()
            throws Exception {

        for (Path layer : List.of(
                CUSTOMER.resolve("application"),
                CUSTOMER.resolve("domain")
        )) {
            assertNoTokens(
                    layer,
                    List.of(
                            "import org.springframework.",
                            "import jakarta.persistence.",
                            "import org.hibernate.",
                            "RestClient",
                            "WebClient",
                            "HttpClient",
                            "@Entity",
                            "@Repository",
                            "@Service",
                            "@Component",
                            "@Transactional",
                            "@RestController",
                            "Thread.sleep("
                    )
            );
        }
    }

    @Test
    void phaseContainsNoUnresolvedImplementationMarkers()
            throws Exception {

        assertNoTokens(
                CUSTOMER,
                List.of(
                        "TODO LOT 4.",
                        "FIXME LOT 4.",
                        "UnsupportedOperationException("
                                + "\"not implemented",
                        "throw new UnsupportedOperationException()"
                )
        );
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
                    () -> "Phase closure violations: " + violations
            );
        }
    }
}
