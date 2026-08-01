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
    private static final Path OUTBOX_ROOT =
            JAVA_ROOT.resolve("infrastructure/outbox");

    @Test
    void lot34AuthorizesOnlyCurrentFoundations()
            throws IOException {
        Map<Path, Set<String>> authorized = Map.of(
                PERSISTENCE_ROOT, Set.of(
                        "PaymentJpaEntity.java",
                        "PaymentPersistenceException.java",
                        "PaymentPersistenceMapper.java",
                        "PaymentRepositoryAdapter.java",
                        "PaymentSpringDataRepository.java",
                        "PaymentStateDocument.java",
                        "package-info.java"
                ),
                AUDIT_ROOT, Set.of(
                        "PaymentAuditAdapter.java",
                        "PaymentAuditEntity.java",
                        "PaymentAuditEntry.java",
                        "PaymentAuditRepository.java",
                        "package-info.java"
                ),
                OUTBOX_ROOT, Set.of(
                        "PaymentDomainEventMapper.java",
                        "PaymentIntegrationMapper.java",
                        "PaymentOutboxEntity.java",
                        "PaymentOutboxMappingException.java",
                        "PaymentOutboxRepository.java",
                        "package-info.java"
                ),
                CONFIGURATION_ROOT, Set.of(
                        "PaymentModuleConfiguration.java",
                        "package-info.java"
                )
        );

        for (Map.Entry<Path, Set<String>> entry
                : authorized.entrySet()) {
            assertTrue(Files.isDirectory(entry.getKey()));

            try (Stream<Path> paths = Files.list(entry.getKey())) {
                List<String> actual = paths
                        .filter(Files::isRegularFile)
                        .filter(path -> path.toString().endsWith(".java"))
                        .map(path -> path.getFileName().toString())
                        .sorted()
                        .toList();

                assertEquals(
                        entry.getValue().stream().sorted().toList(),
                        actual,
                        () -> "Unauthorized files in " + entry.getKey()
                );
            }
        }
    }

    @Test
    void lot34StillForbidsPrematureBackendComponents()
            throws IOException {
        Set<String> forbiddenSuffixes = Set.of(
                "Controller.java",
                "Service.java",
                "Properties.java",
                "Listener.java",
                "Consumer.java",
                "Publisher.java",
                "Scheduler.java"
        );

        try (Stream<Path> paths = Files.walk(JAVA_ROOT)) {
            List<Path> violations = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> !path.startsWith(DOMAIN_ROOT))
                    .filter(path -> !path.startsWith(PERSISTENCE_ROOT))
                    .filter(path -> !path.startsWith(AUDIT_ROOT))
                    .filter(path -> !path.startsWith(OUTBOX_ROOT))
                    .filter(path -> !path.startsWith(CONFIGURATION_ROOT))
                    .filter(path -> !path.getFileName().toString()
                            .equals("PaymentModule.java"))
                    .filter(path -> !path.getFileName().toString()
                            .equals("package-info.java"))
                    .filter(path -> forbiddenSuffixes.stream().anyMatch(
                            suffix -> path.getFileName().toString()
                                    .endsWith(suffix)
                    ))
                    .toList();

            assertEquals(
                    List.of(),
                    violations,
                    "Lot 3.4 must not introduce services, controllers, "
                            + "publishers, consumers, listeners or schedulers"
            );
        }
    }

    @Test
    void outboxRemainsTransportNeutral() throws IOException {
        List<String> forbiddenTokens = List.of(
                "KafkaTemplate",
                "KafkaProducer",
                "org.springframework.kafka",
                "@KafkaListener",
                "@Scheduled",
                "OutboxRelay",
                "OutboxPublisher"
        );

        try (Stream<Path> paths = Files.walk(OUTBOX_ROOT)) {
            List<String> violations = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .flatMap(path -> violations(path, forbiddenTokens).stream())
                    .toList();

            assertEquals(List.of(), violations);
        }
    }

    @Test
    void paymentModuleConfigurationIsInConfigurationLayer()
            throws IOException {
        Path expected = CONFIGURATION_ROOT.resolve(
                "PaymentModuleConfiguration.java"
        );
        Path forbidden = APPLICATION_ROOT.resolve(
                "PaymentModuleConfiguration.java"
        );

        assertTrue(Files.isRegularFile(expected));
        assertFalse(Files.exists(forbidden));

        String source = Files.readString(expected);
        assertTrue(source.contains("@AutoConfiguration"));
        assertTrue(source.contains("PaymentOutboxEntity.class"));
        assertTrue(source.contains("PaymentOutboxRepository.class"));
        assertFalse(source.contains("@SpringBootApplication"));
    }

    @Test
    void applicationLayerContainsNoSpringConfiguration()
            throws IOException {
        if (!Files.isDirectory(APPLICATION_ROOT)) {
            return;
        }

        List<String> forbiddenTokens = List.of(
                "@AutoConfiguration",
                "@Configuration",
                "@EntityScan",
                "@EnableJpaRepositories"
        );

        try (Stream<Path> paths = Files.walk(APPLICATION_ROOT)) {
            List<String> violations = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .flatMap(path -> violations(path, forbiddenTokens).stream())
                    .toList();

            assertEquals(List.of(), violations);
        }
    }

    @Test
    void domainRemainsFrameworkFree() throws IOException {
        List<String> forbiddenTokens = List.of(
                "import org.springframework.",
                "import jakarta.persistence.",
                "import org.hibernate.",
                "import com.sixpay.payment.infrastructure.",
                "import com.sixpay.payment.configuration."
        );

        try (Stream<Path> paths = Files.walk(DOMAIN_ROOT)) {
            List<String> violations = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .flatMap(path -> violations(path, forbiddenTokens).stream())
                    .toList();

            assertTrue(
                    violations.isEmpty(),
                    () -> "Payment domain violations: " + violations
            );
        }
    }

    @Test
    void paymentModuleRemainsNonExecutable()
            throws IOException {
        String source = Files.readString(
                JAVA_ROOT.resolve("PaymentModule.java")
        );
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
