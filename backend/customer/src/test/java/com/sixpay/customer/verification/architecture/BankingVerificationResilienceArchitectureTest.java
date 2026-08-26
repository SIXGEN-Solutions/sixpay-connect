package com.sixpay.customer.verification.architecture;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BankingVerificationResilienceArchitectureTest {

    private static final Path DOMAIN_ROOT = Path.of(
            "src/main/java/com/sixpay/customer/verification/domain"
    );

    private static final Path RETRY_CLASS = Path.of(
            "src/main/java/com/sixpay/customer/verification/"
                    + "infrastructure/banking/retry/"
                    + "RetryingBankingCustomerVerificationAdapter.java"
    );

    private static final Path OBSERVATION_CLASS = Path.of(
            "src/main/java/com/sixpay/customer/verification/"
                    + "infrastructure/banking/observability/"
                    + "BankingVerificationObservation.java"
    );

    @Test
    void retryNeverEntersTheDomain() throws Exception {
        try (var paths = Files.walk(DOMAIN_ROOT)) {
            var violations = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .flatMap(path -> {
                        try {
                            String source = Files.readString(path);

                            return List.of(
                                            "import org.springframework.retry.",
                                            "import io.github.resilience4j.retry.",
                                            "import com.sixpay.customer.verification."
                                                    + "infrastructure.banking.retry.",
                                            "@Retryable",
                                            "RetryTemplate",
                                            "RetryRegistry",
                                            "RetryConfig",
                                            "RetryingBankingCustomerVerificationAdapter",
                                            "RetrySleeper",
                                            "retryBackoff",
                                            "maxAttempts",
                                            "Thread.sleep(",
                                            "TimeUnit.sleep("
                                    )
                                    .stream()
                                    .filter(source::contains)
                                    .map(token ->
                                            path + " contains forbidden retry concept "
                                                    + token
                                    );
                        } catch (Exception exception) {
                            throw new IllegalStateException(
                                    "Cannot inspect " + path,
                                    exception
                            );
                        }
                    })
                    .toList();

            assertTrue(
                    violations.isEmpty(),
                    () -> "Retry leaked into domain: " + violations
            );
        }
    }

    @Test
    void retryDecoratorUsesOnlyInternalErrorRetryability()
            throws Exception {

        String source = Files.readString(RETRY_CLASS);

        assertTrue(
                source.contains("failure.retryable()"),
                "Retry decorator must use internal retryability"
        );

        assertFalse(
                source.contains("AmplitudeClientException"),
                "Retry decorator must not depend on Amplitude errors"
        );

        assertFalse(
                source.contains("HttpStatus"),
                "Retry decorator must not classify HTTP statuses"
        );
    }

    @Test
    void metricTagsExcludeSensitiveAndUnboundedIdentifiers()
            throws Exception {

        String source = Files.readString(OBSERVATION_CLASS);

        for (String forbidden : List.of(
                ".tag(\"verificationId\"",
                ".tag(\"correlationId\"",
                ".tag(\"niu\"",
                ".tag(\"account\"",
                ".tag(\"customer\""
        )) {
            assertFalse(
                    source.contains(forbidden),
                    () -> "Unbounded or sensitive metric tag found: "
                            + forbidden
            );
        }
    }
}