package com.sixpay.bootstrap.readiness;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Phase5ReadinessArchitectureTest {

    private static final Path REPOSITORY_ROOT =
            Path.of("../..");

    @Test
    void bootstrapComposesAllPhase5BusinessModules()
            throws Exception {
        String pom = Files.readString(
                Path.of("pom.xml")
        );

        for (String artifact : List.of(
                "payment",
                "customer",
                "accounting",
                "notification",
                "integration",
                "security"
        )) {
            assertTrue(
                    pom.contains(
                            "<artifactId>"
                                    + artifact
                                    + "</artifactId>"
                    ),
                    () -> "Bootstrap is missing "
                            + artifact
            );
        }
    }

    @Test
    void modularMonolithKeepsObservedCustomerInProcess()
            throws Exception {
        String source = Files.readString(
                Path.of(
                        "src/main/resources/"
                                + "application.yml"
                )
        );

        assertTrue(
                source.contains(
                        "transport: internal"
                )
        );
    }

    @Test
    void phase5ProviderAdaptersStayInOwningModules() {
        for (String path : List.of(
                "backend/payment/src/main/java/"
                        + "com/sixpay/payment/infrastructure/"
                        + "banking/amplitude",
                "backend/customer/src/main/java/"
                        + "com/sixpay/customer/verification/"
                        + "infrastructure/banking",
                "backend/accounting/src/main/java/"
                        + "com/sixpay/accounting/infrastructure/"
                        + "accountingapi",
                "backend/notification/src/main/java/"
                        + "com/sixpay/notification/infrastructure/"
                        + "operational"
        )) {
            assertTrue(
                    Files.isDirectory(
                            REPOSITORY_ROOT.resolve(path)
                    ),
                    () -> "Missing implementation: "
                            + path
            );
        }
    }

    @Test
    void integrationModuleDoesNotOwnProviderSpecificMappings()
            throws Exception {
        Path integration =
                REPOSITORY_ROOT.resolve(
                        "backend/integration/src/main/java"
                );

        if (!Files.isDirectory(integration)) {
            return;
        }

        try (var paths = Files.walk(integration)) {
            var violations = paths
                    .filter(Files::isRegularFile)
                    .filter(path ->
                            path.toString()
                                    .endsWith(".java")
                    )
                    .filter(path -> {
                        try {
                            String source =
                                    Files.readString(path);

                            return source.contains("TresorPay")
                                    || source.contains("Amplitude")
                                    || source.contains(
                                    "AccountingBatchRequestDto"
                            )
                                    || source.contains(
                                    "JavaMailSender"
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
                    () -> "Provider-specific concepts leaked "
                            + "into integration: "
                            + violations
            );
        }
    }

    @Test
    void readinessDocumentationAndRunbookExist() {
        for (String path : List.of(
                "documentation/architecture/integration/"
                        + "phase5-readiness.md",
                "documentation/architecture/integration/"
                        + "phase5-e2e-scenarios.md",
                "documentation/runbooks/integration/"
                        + "PHASE5_E2E_READINESS.md"
        )) {
            assertTrue(
                    Files.isRegularFile(
                            REPOSITORY_ROOT.resolve(path)
                    ),
                    () -> "Missing readiness artifact: "
                            + path
            );
        }
    }

    @Test
    void noProductionReadinessClaimIsHardCoded()
            throws Exception {
        String readiness =
                Files.readString(
                        REPOSITORY_ROOT.resolve(
                                "documentation/architecture/"
                                        + "integration/"
                                        + "phase5-readiness.md"
                        )
                );

        assertFalse(
                readiness.contains(
                        "PRODUCTION_READY: true"
                )
        );

        assertTrue(
                readiness.contains(
                        "EXTERNAL_SANDBOX_CERTIFICATION"
                )
        );
    }
}
