package com.sixpay.payment.infrastructure.outbox;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentOutboxArchitectureTest {

    private static final Path OUTBOX_ROOT = Path.of(
            "src/main/java/com/sixpay/payment/infrastructure/outbox"
    );

    @Test
    void lot34ContainsOnlyAuthorizedOutboxTypes()
            throws IOException {
        Set<String> authorized = Set.of(
                "PaymentDomainEventMapper.java",
                "PaymentIntegrationMapper.java",
                "PaymentOutboxEntity.java",
                "PaymentOutboxMappingException.java",
                "PaymentOutboxRepository.java",
                "package-info.java"
        );

        try (Stream<Path> paths = Files.list(OUTBOX_ROOT)) {
            List<String> actual = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .map(path -> path.getFileName().toString())
                    .sorted()
                    .toList();

            assertEquals(
                    authorized.stream().sorted().toList(),
                    actual
            );
        }
    }

    @Test
    void lot34ContainsNoKafkaOrPublicationComponent()
            throws IOException {
        List<String> forbiddenTokens = List.of(
                "KafkaTemplate",
                "KafkaProducer",
                "org.springframework.kafka",
                "@KafkaListener",
                "ApplicationEventPublisher",
                "@Scheduled",
                "OutboxRelay",
                "OutboxPublisher"
        );

        try (Stream<Path> paths = Files.walk(OUTBOX_ROOT)) {
            List<String> violations = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .flatMap(path -> {
                        try {
                            String source = Files.readString(path);
                            return forbiddenTokens.stream()
                                    .filter(source::contains)
                                    .map(token -> path + " contains " + token);
                        } catch (IOException exception) {
                            throw new IllegalStateException(exception);
                        }
                    })
                    .toList();

            assertTrue(
                    violations.isEmpty(),
                    () -> "Premature publication components: " + violations
            );
        }
    }
}
