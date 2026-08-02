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

class PaymentObservabilityArchitectureTest {

    private static final Path ROOT = Path.of(
            "src/main/java/com/sixpay/payment/"
                    + "infrastructure/observability"
    );

    @Test
    void metricsDoNotUseHighCardinalityPaymentData()
            throws IOException {
        String metrics = Files.readString(
                ROOT.resolve("PaymentMetrics.java")
        );

        for (String forbidden : List.of(
                "paymentId",
                "externalPaymentReference",
                "accountReference",
                "customerId",
                "partnerSubject"
        )) {
            assertFalse(
                    metrics.contains(".tag(\"" + forbidden),
                    () -> "High-cardinality metric tag: "
                            + forbidden
            );
        }
    }

    @Test
    void observabilityNeverLogsSensitivePayloads()
            throws IOException {
        List<String> forbidden = List.of(
                "debtorAccount",
                "statePayload",
                "responsePayload",
                "accessToken",
                "authorizationHeader",
                "customerIdentifier"
        );

        try (Stream<Path> paths = Files.walk(ROOT)) {
            List<String> violations = paths
                    .filter(Files::isRegularFile)
                    .filter(path ->
                            path.toString().endsWith(".java")
                    )
                    .flatMap(path -> {
                        try {
                            String source =
                                    Files.readString(path);
                            return forbidden.stream()
                                    .filter(token ->
                                            source.contains(
                                                    "LOGGER."
                                            )
                                                    && source.contains(
                                                            token
                                                    )
                                    )
                                    .map(token ->
                                            path + " may log " + token
                                    );
                        } catch (IOException exception) {
                            throw new IllegalStateException(
                                    exception
                            );
                        }
                    })
                    .toList();

            assertEquals(List.of(), violations);
        }
    }

    @Test
    void healthUsesSpringBootFourContributorPackage()
            throws IOException {
        String source = Files.readString(
                ROOT.resolve(
                        "PaymentHealthIndicator.java"
                )
        );

        assertTrue(source.contains(
                "org.springframework.boot.health.contributor"
        ));
        assertFalse(source.contains(
                "org.springframework.boot.actuate.health"
        ));
    }

    @Test
    void tracingUsesObservationRegistry()
            throws IOException {
        String source = Files.readString(
                ROOT.resolve(
                        "PaymentObservabilityAspect.java"
                )
        );

        assertTrue(source.contains("ObservationRegistry"));
        assertTrue(source.contains(
                "sixpay.payment.operation"
        ));
    }
}
