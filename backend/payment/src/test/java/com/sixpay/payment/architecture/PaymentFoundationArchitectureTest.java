package com.sixpay.payment.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentFoundationArchitectureTest {

    private static final Path JAVA_ROOT =
            Path.of("src/main/java/com/sixpay/payment");

    private static final Path DOMAIN_ROOT =
            JAVA_ROOT.resolve("domain");

    private static final Path APPLICATION_ROOT =
            JAVA_ROOT.resolve("application");

    private static final Path CONFIGURATION_ROOT =
            JAVA_ROOT.resolve("configuration");

    private static final Path PERSISTENCE_ROOT =
            JAVA_ROOT.resolve("infrastructure/persistence");

    private static final Path AUDIT_ROOT =
            JAVA_ROOT.resolve("infrastructure/audit");

    @Test
    void lot33AuthorizesOnlyPersistenceAndAuditFoundations()
            throws IOException {

        Map<Path, Set<String>> authorizedByPackage = Map.of(
                PERSISTENCE_ROOT,
                Set.of(
                        "PaymentJpaEntity.java",
                        "PaymentPersistenceException.java",
                        "PaymentPersistenceMapper.java",
                        "PaymentRepositoryAdapter.java",
                        "PaymentSpringDataRepository.java",
                        "PaymentStateDocument.java",
                        "package-info.java"
                ),
                AUDIT_ROOT,
                Set.of(
                        "PaymentAuditAdapter.java",
                        "PaymentAuditEntity.java",
                        "PaymentAuditEntry.java",
                        "PaymentAuditRepository.java",
                        "package-info.java"
                ),
                CONFIGURATION_ROOT,
                Set.of(
                        "PaymentModuleConfiguration.java",
                        "package-info.java"
                )
        );

        for (Map.Entry<Path, Set<String>> entry
                : authorizedByPackage.entrySet()) {

            Path packageRoot = entry.getKey();

            assertTrue(
                    Files.isDirectory(packageRoot),
                    () -> "Missing authorized package: "
                            + packageRoot
            );

            List<String> actual;

            try (Stream<Path> paths = Files.list(packageRoot)) {
                actual = paths
                        .filter(Files::isRegularFile)
                        .filter(path ->
                                path.toString().endsWith(".java")
                        )
                        .map(path ->
                                path.getFileName().toString()
                        )
                        .sorted()
                        .toList();
            }

            assertEquals(
                    entry.getValue()
                            .stream()
                            .sorted()
                            .toList(),
                    actual,
                    () -> "Unauthorized files in "
                            + packageRoot
            );
        }
    }

    @Test
    void paymentModuleConfigurationIsInConfigurationLayer()
            throws IOException {

        Path expectedConfiguration =
                CONFIGURATION_ROOT.resolve(
                        "PaymentModuleConfiguration.java"
                );

        Path forbiddenApplicationConfiguration =
                APPLICATION_ROOT.resolve(
                        "PaymentModuleConfiguration.java"
                );

        assertTrue(
                Files.isRegularFile(expectedConfiguration),
                "PaymentModuleConfiguration must be placed "
                        + "in the configuration package"
        );

        assertFalse(
                Files.exists(forbiddenApplicationConfiguration),
                "PaymentModuleConfiguration must not be placed "
                        + "in the application package"
        );

        String source = Files.readString(
                expectedConfiguration
        );

        assertTrue(
                source.contains(
                        "package com.sixpay.payment.configuration;"
                )
        );

        assertTrue(
                source.contains("@AutoConfiguration")
        );

        assertTrue(
                source.contains("@EntityScan")
        );

        assertTrue(
                source.contains("@EnableJpaRepositories")
        );

        assertFalse(
                source.contains("@SpringBootApplication")
        );
    }

    @Test
    void lot33StillForbidsPrematureBackendComponents()
            throws IOException {

        Set<String> forbiddenTypeSuffixes = Set.of(
                "Controller.java",
                "Service.java",
                "Properties.java",
                "Listener.java",
                "Consumer.java",
                "Publisher.java",
                "Scheduler.java"
        );

        List<Path> violations;

        try (Stream<Path> paths = Files.walk(JAVA_ROOT)) {
            violations = paths
                    .filter(Files::isRegularFile)
                    .filter(path ->
                            path.toString().endsWith(".java")
                    )
                    .filter(path ->
                            !path.startsWith(DOMAIN_ROOT)
                    )
                    .filter(path ->
                            !path.startsWith(PERSISTENCE_ROOT)
                    )
                    .filter(path ->
                            !path.startsWith(AUDIT_ROOT)
                    )
                    .filter(path ->
                            !path.startsWith(CONFIGURATION_ROOT)
                    )
                    .filter(path ->
                            !path.getFileName()
                                    .toString()
                                    .equals("PaymentModule.java")
                    )
                    .filter(path ->
                            !path.getFileName()
                                    .toString()
                                    .equals("package-info.java")
                    )
                    .filter(path ->
                            forbiddenTypeSuffixes.stream()
                                    .anyMatch(suffix ->
                                            path.getFileName()
                                                    .toString()
                                                    .endsWith(
                                                            suffix
                                                    )
                                    )
                    )
                    .toList();
        }

        assertEquals(
                List.of(),
                violations,
                "Lot 3.3 must not introduce application services, "
                        + "controllers, messaging components, "
                        + "properties or schedulers"
        );
    }

    @Test
    void applicationLayerContainsNoSpringConfiguration()
            throws IOException {

        if (!Files.isDirectory(APPLICATION_ROOT)) {
            return;
        }

        List<Path> violations;

        try (Stream<Path> paths =
                     Files.walk(APPLICATION_ROOT)) {

            violations = paths
                    .filter(Files::isRegularFile)
                    .filter(path ->
                            path.toString().endsWith(".java")
                    )
                    .filter(path -> {
                        try {
                            String source =
                                    Files.readString(path);

                            return source.contains(
                                    "@AutoConfiguration"
                            ) || source.contains(
                                    "@Configuration"
                            ) || source.contains(
                                    "@EntityScan"
                            ) || source.contains(
                                    "@EnableJpaRepositories"
                            );

                        } catch (IOException exception) {
                            throw new IllegalStateException(
                                    exception
                            );
                        }
                    })
                    .toList();
        }

        assertEquals(
                List.of(),
                violations,
                "The application layer must not contain "
                        + "Spring module configuration"
        );
    }

    @Test
    void domainRemainsFrameworkFree()
            throws IOException {

        List<String> forbiddenTokens = List.of(
                "import org.springframework.",
                "import jakarta.persistence.",
                "import org.hibernate.",
                "import com.sixpay.payment.infrastructure.",
                "import com.sixpay.payment.configuration."
        );

        List<String> violations;

        try (Stream<Path> paths =
                     Files.walk(DOMAIN_ROOT)) {

            violations = paths
                    .filter(Files::isRegularFile)
                    .filter(path ->
                            path.toString().endsWith(".java")
                    )
                    .flatMap(path ->
                            forbiddenTokens.stream()
                                    .filter(token -> {
                                        try {
                                            return Files.readString(
                                                    path
                                            ).contains(token);

                                        } catch (
                                                IOException exception
                                        ) {
                                            throw new IllegalStateException(
                                                    exception
                                            );
                                        }
                                    })
                                    .map(token ->
                                            path + " contains "
                                                    + token
                                    )
                    )
                    .toList();
        }

        assertTrue(
                violations.isEmpty(),
                () -> "Payment domain framework violations: "
                        + violations
        );
    }

    @Test
    void paymentModuleRemainsNonExecutable()
            throws IOException {

        String source = Files.readString(
                JAVA_ROOT.resolve("PaymentModule.java")
        );

        assertFalse(
                source.contains("@SpringBootApplication")
        );

        assertFalse(
                source.contains("public static void main(")
        );
    }
}