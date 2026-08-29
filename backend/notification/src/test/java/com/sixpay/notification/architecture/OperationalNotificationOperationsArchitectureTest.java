package com.sixpay.notification.architecture;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OperationalNotificationOperationsArchitectureTest {

    private static final Path ROOT = Path.of(
            "src/main/java/com/sixpay/notification"
    );

    @Test
    void controlledReplayOnlyTargetsDeadLetteredRows()
            throws Exception {
        String repository = Files.readString(
                ROOT.resolve(
                        "infrastructure/operational/persistence/"
                                + "OperationalNotificationSpringDataRepository.java"
                )
        );

        assertTrue(
                repository.contains(
                        "and status = 'DEAD_LETTERED'"
                )
        );

        assertTrue(
                repository.contains(
                        "cycle_attempt_count = 0"
                )
        );

        assertTrue(
                repository.contains(
                        "replay_count = replay_count + 1"
                )
        );

        assertFalse(
                repository.contains(
                        "insert into sixpay.operational_notification_deliveries"
                                + " (select"
                )
        );
    }

    @Test
    void purgeDeletesOnlyTerminalStatuses()
            throws Exception {
        String repository = Files.readString(
                ROOT.resolve(
                        "infrastructure/operational/persistence/"
                                + "OperationalNotificationSpringDataRepository.java"
                )
        );

        assertTrue(
                repository.contains(
                        "status = 'DELIVERED'"
                )
        );
        assertTrue(
                repository.contains(
                        "status in ('FAILED_PERMANENT', 'DEAD_LETTERED')"
                )
        );

        String purgeQuery = repository.substring(
                repository.indexOf(
                        "delete from sixpay.operational_notification_deliveries"
                )
        );

        for (String forbidden : List.of(
                "status = 'PENDING'",
                "status = 'DISPATCHING'",
                "status = 'ACCEPTED'",
                "status = 'FAILED_RETRYABLE'"
        )) {
            assertFalse(
                    purgeQuery.contains(forbidden),
                    () -> "Non-terminal status leaked into purge: "
                            + forbidden
            );
        }
    }

    @Test
    void statusViewContainsLogicalRecipientOnly()
            throws Exception {
        String source = Files.readString(
                ROOT.resolve(
                        "application/port/input/"
                                + "OperationalNotificationStatusView.java"
                )
        );

        assertTrue(
                source.contains(
                        "String recipientReference"
                )
        );

        for (String forbidden : List.of(
                "emailAddress",
                "recipientEmail",
                "smtpPassword",
                "templateBody"
        )) {
            assertFalse(
                    source.contains(forbidden)
            );
        }
    }

    @Test
    void operationsRemainProviderNeutralAndKafkaFree()
            throws Exception {
        String source =
                readAll(
                        ROOT.resolve(
                                "application/service"
                        )
                )
                        + readAll(
                        ROOT.resolve(
                                "infrastructure/operational/observability"
                        )
                );

        for (String forbidden : List.of(
                "KafkaTemplate",
                "@KafkaListener",
                "JavaMailSender",
                "com.sixpay.payment.",
                "com.sixpay.accounting."
        )) {
            assertFalse(
                    source.contains(forbidden),
                    () -> "Operational concern leaked dependency: "
                            + forbidden
            );
        }
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
