package com.sixpay.payment.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentEndToEndArchitectureTest {

    private static final Path PAYMENT_MAIN =
            Path.of(
                    "src/main/java/com/sixpay/payment"
            );

    private static final Path E2E_TEST =
            Path.of(
                    "src/test/java/com/sixpay/payment/"
                            + "infrastructure/outbox/"
                            + "PaymentEndToEndIntegrationIT.java"
            );

    @Test
    void paymentProductionCodeDoesNotCoupleToAccountingOrNotification()
            throws IOException {
        try (Stream<Path> paths = Files.walk(PAYMENT_MAIN)) {
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
                                    "com.sixpay.accounting",
                                    "com.sixpay.notification"
                            ).stream()
                                    .filter(source::contains)
                                    .map(token ->
                                            path + " contains " + token
                                    );
                        } catch (IOException exception) {
                            throw new IllegalStateException(
                                    exception
                            );
                        }
                    })
                    .toList();

            assertEquals(
                    List.of(),
                    violations,
                    "Payment must integrate through events, "
                            + "not direct module dependencies"
            );
        }
    }

    @Test
    void e2eUsesDurableOutboxAndIntegrationEnvelope()
            throws IOException {
        String source = Files.readString(E2E_TEST);

        assertTrue(
                source.contains(
                        "PaymentOutboxRepository"
                )
        );
        assertTrue(
                source.contains(
                        "PaymentIntegrationMapper"
                )
        );
        assertTrue(
                source.contains(
                        "IntegrationEventEnvelope"
                )
        );
        assertTrue(
                source.contains(
                        "PostgreSQLContainer"
                )
        );
    }

    @Test
    void e2eDoesNotPretendToUseKafkaOrExternalHttp()
            throws IOException {
        String source = Files.readString(E2E_TEST);

        assertFalse(source.contains("KafkaTemplate"));
        assertFalse(source.contains("RestClient"));
        assertFalse(source.contains("WebClient"));
        assertFalse(source.contains("WireMock"));
    }
}
