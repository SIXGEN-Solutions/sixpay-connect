package com.sixpay.customer.verification.infrastructure.banking.architecture;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AmplitudeInfrastructureArchitectureTest {

    private static final Path ROOT = Path.of(
            "src/main/java/com/sixpay/customer/verification/"
                    + "infrastructure/banking"
    );

    @Test
    void externalDtosRemainInsideInfrastructure() throws Exception {
        Path customerRoot = Path.of(
                "src/main/java/com/sixpay/customer"
        );

        try (var paths = Files.walk(customerRoot)) {
            var violations = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> !path.startsWith(ROOT))
                    .flatMap(path -> {
                        try {
                            String source = Files.readString(path);
                            return List.of(
                                            "AmplitudeCustomerVerificationRequest",
                                            "AmplitudeCustomerVerificationResponse",
                                            "AmplitudeVerificationCheckResponse",
                                            "AmplitudeErrorResponse"
                                    )
                                    .stream()
                                    .filter(source::contains)
                                    .map(token -> path + " exposes " + token);
                        } catch (Exception exception) {
                            throw new IllegalStateException(exception);
                        }
                    })
                    .toList();

            assertTrue(
                    violations.isEmpty(),
                    () -> "External DTO leakage: " + violations
            );
        }
    }

    @Test
    void httpClientDoesNotDecideGlobalOutcome()
            throws Exception {

        String source = Files.readString(
                ROOT.resolve(
                        "client/AmplitudeCustomerVerificationClient.java"
                )
        );

        for (String forbidden : List.of(
                "VerificationOutcome",
                "VERIFIED",
                "REJECTED",
                "INDETERMINATE",
                "VerificationOutcomePolicy"
        )) {
            assertFalse(
                    source.contains(forbidden),
                    () -> "HTTP client decides outcome: " + forbidden
            );
        }
    }
}
