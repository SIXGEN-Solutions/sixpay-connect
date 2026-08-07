package com.sixpay.notification.architecture;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OperationalNotificationEmailArchitectureTest {

    private static final Path ROOT = Path.of(
            "src/main/java/com/sixpay/notification"
    );

    @Test
    void smtpIsBehindOperationalDeliveryGateway()
            throws Exception {
        String source = Files.readString(
                ROOT.resolve(
                        "infrastructure/operational/email/"
                                + "OperationalSmtpNotificationDeliveryGateway.java"
                )
        );

        assertTrue(
                source.contains(
                        "implements OperationalNotificationDeliveryGateway"
                )
        );

        assertTrue(
                source.contains(
                        "JavaMailSender"
                )
        );
    }

    @Test
    void domainAndApplicationContainNoSpringMailDependency()
            throws Exception {
        String sources =
                readAll(ROOT.resolve("domain"))
                        + readAll(
                        ROOT.resolve("application")
                );

        for (String forbidden : List.of(
                "JavaMailSender",
                "SimpleMailMessage",
                "org.springframework.mail."
        )) {
            assertFalse(
                    sources.contains(forbidden),
                    () -> "SMTP leaked outside infrastructure: "
                            + forbidden
            );
        }
    }

    @Test
    void operationalEmailIntroducesNoKafkaTransport()
            throws Exception {
        String sources = readAll(
                ROOT.resolve(
                        "infrastructure/operational/email"
                )
        );

        for (String forbidden : List.of(
                "KafkaTemplate",
                "@KafkaListener",
                "springframework.kafka"
        )) {
            assertFalse(
                    sources.contains(forbidden),
                    () -> "Email adapter must not require Kafka: "
                            + forbidden
            );
        }
    }

    @Test
    void smtpSuccessDoesNotClaimMailboxDelivery()
            throws Exception {
        String source = Files.readString(
                ROOT.resolve(
                        "infrastructure/operational/email/"
                                + "OperationalSmtpNotificationDeliveryGateway.java"
                )
        );

        assertTrue(
                source.contains(
                        "NotificationDispatchResult.accepted"
                )
        );

        assertFalse(
                source.contains(
                        "NotificationDispatchResult.delivered"
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
