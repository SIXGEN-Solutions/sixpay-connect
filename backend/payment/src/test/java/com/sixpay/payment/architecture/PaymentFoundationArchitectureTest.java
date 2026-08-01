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

    private static final Map<String, List<String>>
            FORBIDDEN_IMPORTS_BY_LAYER = Map.of(
                    "domain", List.of(
                            "import com.sixpay.payment.api.",
                            "import com.sixpay.payment.application.",
                            "import com.sixpay.payment.configuration.",
                            "import com.sixpay.payment.events.",
                            "import com.sixpay.payment.infrastructure."
                    ),
                    "application", List.of(
                            "import com.sixpay.payment.api.",
                            "import com.sixpay.payment.configuration.",
                            "import com.sixpay.payment.infrastructure.",
                            "import jakarta.persistence.",
                            "import jakarta.servlet."
                    ),
                    "api", List.of(
                            "import com.sixpay.payment.configuration.",
                            "import com.sixpay.payment.infrastructure.",
                            "import jakarta.persistence."
                    ),
                    "events", List.of(
                            "import com.sixpay.payment.api.",
                            "import com.sixpay.payment.configuration.",
                            "import com.sixpay.payment.infrastructure.",
                            "import jakarta.persistence."
                    ),
                    "infrastructure", List.of(
                            "import com.sixpay.payment.api."
                    )
            );

    private static final Set<String> REQUIRED_BOUNDARIES =
            Set.of(
                    "api",
                    "application",
                    "configuration",
                    "domain",
                    "events",
                    "infrastructure"
            );

    @Test
    void canonicalVerticalBoundariesArePresent()
            throws IOException {

        Set<String> actual;

        try (Stream<Path> paths = Files.list(JAVA_ROOT)) {
            actual = paths
                    .filter(Files::isDirectory)
                    .map(path -> path.getFileName().toString())
                    .collect(java.util.stream.Collectors.toSet());
        }

        assertTrue(
                actual.containsAll(REQUIRED_BOUNDARIES),
                () -> "Missing Payment boundaries: "
                        + REQUIRED_BOUNDARIES.stream()
                        .filter(boundary -> !actual.contains(boundary))
                        .sorted()
                        .toList()
        );
    }

    @Test
    void packageDocumentationDefinesEveryNewBoundary()
            throws IOException {

        for (String boundary : REQUIRED_BOUNDARIES) {
            if ("domain".equals(boundary)) {
                continue;
            }

            Path packageInfo = JAVA_ROOT
                    .resolve(boundary)
                    .resolve("package-info.java");

            assertTrue(
                    Files.isRegularFile(packageInfo),
                    () -> "Missing package documentation: " + packageInfo
            );
        }
    }

    @Test
    void sourceDependenciesPointInward()
            throws IOException {

        for (Map.Entry<String, List<String>> rule
                : FORBIDDEN_IMPORTS_BY_LAYER.entrySet()) {

            Path layerRoot = JAVA_ROOT.resolve(rule.getKey());
            if (!Files.isDirectory(layerRoot)) {
                continue;
            }

            List<String> violations;

            try (Stream<Path> paths = Files.walk(layerRoot)) {
                violations = paths
                        .filter(Files::isRegularFile)
                        .filter(path -> path.toString().endsWith(".java"))
                        .flatMap(path -> violations(
                                path,
                                rule.getValue()
                        ).stream())
                        .toList();
            }

            assertTrue(
                    violations.isEmpty(),
                    () -> "Payment layer dependency violations: "
                            + violations
            );
        }
    }

    @Test
    void lot32ContainsOnlyAuthorizedPersistenceImplementation()
            throws IOException {

        Set<String> authorizedPersistenceTypes = Set.of(
                "PaymentJpaEntity.java",
                "PaymentPersistenceException.java",
                "PaymentPersistenceMapper.java",
                "PaymentRepositoryAdapter.java",
                "PaymentSpringDataRepository.java",
                "PaymentStateDocument.java",
                "package-info.java"
        );

        Set<String> forbiddenTypeSuffixes = Set.of(
                "Controller.java",
                "Service.java",
                "Configuration.java",
                "Properties.java",
                "Listener.java",
                "Consumer.java",
                "Publisher.java",
                "Scheduler.java"
        );

        List<Path> unauthorizedPersistenceTypes;
        List<Path> prematureBackendTypes;

        Path persistenceRoot = JAVA_ROOT.resolve(
                "infrastructure/persistence"
        );

        try (Stream<Path> paths = Files.walk(persistenceRoot)) {
            unauthorizedPersistenceTypes = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> !authorizedPersistenceTypes.contains(
                            path.getFileName().toString()
                    ))
                    .toList();
        }

        try (Stream<Path> paths = Files.walk(JAVA_ROOT)) {
            prematureBackendTypes = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> !path.startsWith(
                            JAVA_ROOT.resolve("domain")
                    ))
                    .filter(path -> !path.startsWith(
                            persistenceRoot
                    ))
                    .filter(path -> !path.getFileName()
                            .toString()
                            .equals("PaymentModule.java"))
                    .filter(path -> !path.getFileName()
                            .toString()
                            .equals("package-info.java"))
                    .filter(path -> forbiddenTypeSuffixes.stream()
                            .anyMatch(suffix ->
                                    path.getFileName()
                                            .toString()
                                            .endsWith(suffix)
                            ))
                    .toList();
        }

        assertEquals(
                List.of(),
                unauthorizedPersistenceTypes,
                "Lot 3.2 must contain only the authorized persistence types"
        );

        assertEquals(
                List.of(),
                prematureBackendTypes,
                "Lot 3.2 must not introduce application, API, messaging, "
                        + "configuration or scheduling components"
        );
    }

    @Test
    void lot32IntroducesOnlyTheFrameworkFreeDomainRepositoryPort()
            throws IOException {

        Path repositoryRoot = JAVA_ROOT.resolve(
                "domain/repository"
        );

        List<String> repositoryFiles;

        try (Stream<Path> paths = Files.list(repositoryRoot)) {
            repositoryFiles = paths
                    .filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .sorted()
                    .toList();
        }

        assertEquals(
                List.of("PaymentRepository.java"),
                repositoryFiles,
                "Lot 3.2 must introduce only the Payment repository port"
        );

        String source = Files.readString(
                repositoryRoot.resolve("PaymentRepository.java")
        );

        assertFalse(source.contains("org.springframework"));
        assertFalse(source.contains("jakarta.persistence"));
        assertFalse(source.contains("PaymentJpaEntity"));
    }

    @Test
    void paymentModuleRemainsNonExecutable()
            throws IOException {

        Path marker = JAVA_ROOT.resolve("PaymentModule.java");
        String source = Files.readString(marker);

        assertFalse(source.contains("@SpringBootApplication"));
        assertFalse(source.contains("public static void main("));
    }

    private static List<String> violations(
            Path path,
            List<String> forbiddenTokens
    ) {
        try {
            String source = Files.readString(path);

            return forbiddenTokens.stream()
                    .filter(source::contains)
                    .map(token -> path + " contains " + token)
                    .toList();
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
