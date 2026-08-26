package com.sixpay.bootstrap.readiness;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Phase6ReadinessArchitectureTest {

    private static final Path ROOT =
            Path.of("../..").normalize();

    @Test
    void phase6CapabilitiesRemainInOwningModules()
            throws Exception {

        assertFile(
                "backend/payment/src/main/java/"
                        + "com/sixpay/payment/api/"
                        + "PaymentQueryController.java"
        );
        assertFile(
                "backend/customer/src/main/java/"
                        + "com/sixpay/customer/observation/api/"
                        + "controller/"
                        + "ObservedCustomerQueryController.java"
        );
        assertFile(
                "backend/reporting/src/main/java/"
                        + "com/sixpay/reporting/api/controller/"
                        + "PaymentAuditQueryController.java"
        );
        assertFile(
                "backend/reporting/src/main/java/"
                        + "com/sixpay/reporting/api/controller/"
                        + "PaymentAuditExportController.java"
        );
    }

    @Test
    void reportingRemainsNonExecutableAndBootstrapComposesIt()
            throws Exception {

        String reportingPom =
                read("backend/reporting/pom.xml");
        String bootstrapPom =
                read("backend/bootstrap/pom.xml");

        assertTrue(
                reportingPom.contains(
                        "<packaging>jar</packaging>"
                )
        );
        assertFalse(
                reportingPom.contains(
                        "spring-boot-maven-plugin"
                )
        );
        assertTrue(
                bootstrapPom.contains(
                        "<artifactId>reporting</artifactId>"
                )
        );
    }

    @Test
    void reportingNeverLoadsOtherBusinessAggregates()
            throws Exception {

        Path reporting = ROOT.resolve(
                "backend/reporting/src/main/java"
        );

        try (var paths = Files.walk(reporting)) {
            List<String> violations = paths
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
                                            "import com.sixpay.customer.",
                                            "import com.sixpay.accounting.",
                                            "import com.sixpay.notification.",
                                            "PaymentRepository",
                                            "ObservedCustomerRepository",
                                            "customer_observed_payment",
                                            "FROM payments "
                                    )
                                    .stream()
                                    .filter(source::contains)
                                    .map(token ->
                                            path + " contains " + token
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
                    () -> "Phase 6 architecture violations: "
                            + violations
            );
        }
    }

    @Test
    void integrationDoesNotOwnInternalBusinessApis()
            throws Exception {

        Path integration = ROOT.resolve(
                "backend/integration"
        );

        if (!Files.isDirectory(integration)) {
            return;
        }

        try (var paths = Files.walk(integration)) {
            List<String> violations = paths
                    .filter(Files::isRegularFile)
                    .filter(path ->
                            path.toString().endsWith(".java")
                    )
                    .filter(path -> {
                        try {
                            String source =
                                    Files.readString(path);
                            return source.contains(
                                    "/internal/api/v1/payments"
                            )
                                    || source.contains(
                                    "/internal/api/v1/"
                                            + "observed-customers"
                            )
                                    || source.contains(
                                    "/internal/api/v1/"
                                            + "payment-audit-"
                            );
                        } catch (Exception exception) {
                            throw new IllegalStateException(
                                    exception
                            );
                        }
                    })
                    .map(Path::toString)
                    .toList();

            assertTrue(
                    violations.isEmpty(),
                    () -> "Internal API ownership leaked "
                            + "into integration: "
                            + violations
            );
        }
    }

    @Test
    void reportingRepositoriesRemainSpringProxyable()
            throws Exception {

        Path reporting = ROOT.resolve(
                "backend/reporting/src/main/java/"
                        + "com/sixpay/reporting"
        );

        try (var paths = Files.walk(reporting)) {
            List<String> violations = paths
                    .filter(Files::isRegularFile)
                    .filter(path ->
                            path.toString().endsWith(".java")
                    )
                    .flatMap(path -> {
                        try {
                            String source =
                                    Files.readString(path);

                            if (source.contains("@Repository")
                                    && source.contains(
                                    "public final class"
                            )) {
                                return java.util.stream.Stream.of(
                                        path
                                                + " is @Repository "
                                                + "and final"
                                );
                            }

                            return java.util.stream.Stream.empty();
                        } catch (Exception exception) {
                            throw new IllegalStateException(
                                    exception
                            );
                        }
                    })
                    .toList();

            assertTrue(
                    violations.isEmpty(),
                    () -> "Non-proxyable Reporting repositories: "
                            + violations
            );
        }
    }

    private static void assertFile(String relative) {
        assertTrue(
                Files.isRegularFile(ROOT.resolve(relative)),
                () -> "Missing Phase 6 asset: " + relative
        );
    }

    private static String read(String relative)
            throws Exception {
        return Files.readString(ROOT.resolve(relative));
    }
}
