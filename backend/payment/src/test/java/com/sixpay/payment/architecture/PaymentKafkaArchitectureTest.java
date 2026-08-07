package com.sixpay.payment.architecture;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class PaymentKafkaArchitectureTest {

    private static final Path PAYMENT_ROOT =
            Path.of(
                    "src/main/java/com/sixpay/payment"
            );

    @Test
    void paymentDomainDoesNotDependOnKafka()
            throws Exception {

        try (var paths = Files.walk(
                PAYMENT_ROOT.resolve("domain")
        )) {
            var violations = paths
                    .filter(Files::isRegularFile)
                    .filter(path ->
                            path.toString().endsWith(".java")
                    )
                    .filter(path -> {
                        try {
                            String source =
                                    Files.readString(path);
                            return source.contains(
                                    "org.springframework.kafka"
                            ) || source.contains(
                                    "KafkaTemplate"
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
                    () -> "Kafka leaked into Payment domain: "
                            + violations
            );
        }
    }

    @Test
    void distributedEventsRemainInfrastructureContracts()
            throws Exception {

        Path root = PAYMENT_ROOT.resolve(
                "infrastructure/event/distributed"
        );

        assertTrue(Files.isDirectory(root));

        String sources;
        try (var paths = Files.walk(root)) {
            sources = paths
                    .filter(Files::isRegularFile)
                    .map(path -> {
                        try {
                            return Files.readString(path);
                        } catch (Exception exception) {
                            throw new IllegalStateException(
                                    exception
                            );
                        }
                    })
                    .reduce("", String::concat);
        }

        assertFalse(
                sources.contains("KafkaTemplate")
        );
        assertFalse(
                sources.contains("@KafkaListener")
        );
    }
}
