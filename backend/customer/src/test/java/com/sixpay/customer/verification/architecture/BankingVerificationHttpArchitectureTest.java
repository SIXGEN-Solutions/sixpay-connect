package com.sixpay.customer.verification.architecture;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BankingVerificationHttpArchitectureTest {

    private static final Path CUSTOMER_ROOT = Path.of(
            "src/main/java/com/sixpay/customer"
    );

    private static final Path PORT_ROOT = Path.of(
            "src/main/java/com/sixpay/customer/verification/"
                    + "application/port/out"
    );

    private static final Path BANKING_INFRASTRUCTURE = Path.of(
            "src/main/java/com/sixpay/customer/verification/"
                    + "infrastructure/banking"
    );

    @Test
    void amplitudeDtosAreAbsentFromThePortApi() throws Exception {
        try (var paths = Files.walk(PORT_ROOT)) {
            var violations = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .flatMap(path -> {
                        try {
                            String source = Files.readString(path);
                            return List.of(
                                            "AmplitudeCustomerVerificationRequest",
                                            "AmplitudeCustomerVerificationResponse",
                                            "AmplitudeVerificationCheckResponse",
                                            "AmplitudeErrorResponse",
                                            "RestClient",
                                            "HttpHeaders",
                                            "HttpStatus"
                                    )
                                    .stream()
                                    .filter(source::contains)
                                    .map(token -> path + " contains " + token);
                        } catch (Exception exception) {
                            throw new IllegalStateException(exception);
                        }
                    })
                    .toList();

            assertTrue(
                    violations.isEmpty(),
                    () -> "External DTO leaked into port API: "
                            + violations
            );
        }
    }

    @Test
    void amplitudeDtosRemainConfinedToBankingInfrastructure()
            throws Exception {

        try (var paths = Files.walk(CUSTOMER_ROOT)) {
            var violations = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> !path.startsWith(
                            BANKING_INFRASTRUCTURE
                    ))
                    .flatMap(path -> {
                        try {
                            String source = Files.readString(path);
                            return List.of(
                                            "AmplitudeCustomerVerificationRequest",
                                            "AmplitudeCustomerVerificationResponse",
                                            "AmplitudeVerificationCheckResponse",
                                            "AmplitudeErrorResponse",
                                            "AmplitudeClientException"
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
                    () -> "External model leakage: " + violations
            );
        }
    }

    @Test
    void domainContainsNoHttpRetryOrMetricsDependency()
            throws Exception {

        Path domain = Path.of(
                "src/main/java/com/sixpay/customer/verification/domain"
        );

        try (var paths = Files.walk(domain)) {
            var violations = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .flatMap(path -> {
                        try {
                            String source = Files.readString(path);
                            return List.of(
                                            "import org.springframework.",
                                            "RestClient",
                                            "HttpClient",
                                            "HttpStatus",
                                            "MeterRegistry",
                                            "Counter",
                                            "Timer.builder",
                                            "@Retryable",
                                            "RetryTemplate",
                                            "RetrySleeper",
                                            "maxAttempts",
                                            "retryBackoff"
                                    )
                                    .stream()
                                    .filter(source::contains)
                                    .map(token -> path + " contains " + token);
                        } catch (Exception exception) {
                            throw new IllegalStateException(exception);
                        }
                    })
                    .toList();

            assertTrue(
                    violations.isEmpty(),
                    () -> "Infrastructure leaked into domain: "
                            + violations
            );
        }
    }

    @Test
    void metricTagsRemainBoundedAndNonSensitive()
            throws Exception {

        String source = Files.readString(
                BANKING_INFRASTRUCTURE.resolve(
                        "observability/"
                                + "BankingVerificationObservation.java"
                )
        );

        for (String forbidden : List.of(
                ".tag(\"verificationId\"",
                ".tag(\"correlationId\"",
                ".tag(\"niu\"",
                ".tag(\"account\"",
                ".tag(\"customer\"",
                ".tag(\"requestId\""
        )) {
            assertFalse(
                    source.contains(forbidden),
                    () -> "Unbounded metric tag found: " + forbidden
            );
        }

        for (String required : List.of(
                "\"institution\"",
                "\"outcome\"",
                "\"error_type\""
        )) {
            assertTrue(
                    source.contains(required),
                    () -> "Required bounded tag missing: " + required
            );
        }
    }
}
