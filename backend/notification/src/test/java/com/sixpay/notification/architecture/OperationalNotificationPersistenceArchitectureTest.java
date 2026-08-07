package com.sixpay.notification.architecture;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OperationalNotificationPersistenceArchitectureTest {

    private static final Path ROOT = Path.of(
            "src/main/java/com/sixpay/notification"
    );

    @Test
    void operationalPersistenceHasFunctionalDeduplicationGuard()
            throws Exception {
        String migration = Files.readString(
                Path.of(
                        "../bootstrap/src/main/resources/db/migration/"
                                + "V202608071300__operational_notifications.sql"
                )
        );

        assertTrue(
                migration.contains(
                        "uk_operational_notification_deduplication"
                )
        );
        assertTrue(
                migration.contains(
                        "UNIQUE (deduplication_key)"
                )
        );
        assertTrue(
                migration.contains(
                        "uk_operational_notification_attempt_number"
                )
        );
    }

    @Test
    void operationalRetryUsesDatabaseDlqNotKafkaTransport()
            throws Exception {
        String sources = readAll(
                ROOT.resolve(
                        "infrastructure/operational"
                )
        );

        for (String forbidden : List.of(
                "KafkaTemplate",
                "@KafkaListener",
                "DeadLetterPublishingRecoverer",
                "springframework.kafka"
        )) {
            assertFalse(
                    sources.contains(forbidden),
                    () -> "Operational Notification "
                            + "must not require Kafka: "
                            + forbidden
            );
        }

        String deliveryStatus = Files.readString(
                ROOT.resolve(
                        "domain/model/"
                                + "NotificationDeliveryStatus.java"
                )
        );

        assertTrue(
                deliveryStatus.contains(
                        "DEAD_LETTERED"
                )
        );
    }

    @Test
    void operationalOrchestrationIsNonThrowingAtIngress()
            throws Exception {
        String source = Files.readString(
                ROOT.resolve(
                        "application/service/"
                                + "OperationalNotificationOrchestrationService.java"
                )
        );

        assertTrue(
                source.contains(
                        "catch (RuntimeException exception)"
                )
        );

        assertTrue(
                source.contains(
                        "NotificationRegistrationResult.failure"
                )
        );
    }

    @Test
    void operationalCapabilityDoesNotDependOnPaymentOrAccounting()
            throws Exception {
        String sources = readAll(
                ROOT.resolve(
                        "application"
                )
        ) + readAll(
                ROOT.resolve(
                        "domain"
                )
        );

        assertFalse(
                sources.contains(
                        "import com.sixpay.payment."
                )
        );

        assertFalse(
                sources.contains(
                        "import com.sixpay.accounting."
                )
        );
    }

    private static String readAll(
            Path root
    ) throws Exception {
        if (!Files.isDirectory(root)) {
            return "";
        }

        try (var paths = Files.walk(root)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(path ->
                            path.toString()
                                    .endsWith(".java")
                    )
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
    }
}
