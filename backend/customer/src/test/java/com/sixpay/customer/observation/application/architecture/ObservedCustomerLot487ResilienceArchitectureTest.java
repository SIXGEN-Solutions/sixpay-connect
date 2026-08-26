package com.sixpay.customer.observation.application.architecture;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ObservedCustomerLot487ResilienceArchitectureTest {

    private static final Path ROOT = Path.of(
            "src/main/java/com/sixpay/customer/observation"
    );

    @Test
    void classifierCoversRetryableAndPermanentFailures()
            throws Exception {

        String classifier = Files.readString(
                ROOT.resolve(
                        "infrastructure/resilience/"
                                + "ObservedCustomerProjectionFailureClassifier.java"
                )
        );

        for (String required : List.of(
                "ObjectOptimisticLockingFailureException",
                "OptimisticLockException",
                "\"40001\"",
                "\"40P01\"",
                "\"23505\"",
                "startsWith(\"08\")",
                "IDEMPOTENCE_RACE",
                "INVALID_PAYLOAD",
                "CONTRADICTORY_IDENTITY",
                "UNKNOWN_STATUS",
                "MISSING_REQUIRED_DATA",
                "PERMANENT_CRYPTOGRAPHY",
                "INCOMPATIBLE_CONTRACT"
        )) {
            assertTrue(
                    classifier.contains(required),
                    () -> "Missing resilience classification: "
                            + required
            );
        }
    }

    @Test
    void retryPolicyIsBoundedInjectedAndHasNoApplicationSleep()
            throws Exception {

        String policy = Files.readString(
                ROOT.resolve(
                        "infrastructure/resilience/"
                                + "ObservedCustomerProjectionRetryPolicy.java"
                )
        );

        for (String required : List.of(
                "maxAttempts",
                "initialBackoff",
                "maxBackoff",
                "multiplier",
                "jitter",
                "ObservedCustomerProjectionBackoff",
                "shouldRetry(",
                "beforeRetry("
        )) {
            assertTrue(
                    policy.contains(required),
                    () -> "Missing bounded retry concept: " + required
            );
        }

        assertFalse(policy.contains("while (true)"));
        assertFalse(policy.contains("for (;;)"));
        assertFalse(policy.contains("Thread.sleep("));

        for (Path layer : List.of(
                ROOT.resolve("application"),
                ROOT.resolve("domain")
        )) {
            assertNoTokens(
                    layer,
                    List.of(
                            "Thread.sleep(",
                            "LockSupport.",
                            "RetryTemplate",
                            "org.springframework.retry",
                            "ObservedCustomerProjectionRetryPolicy"
                    )
            );
        }
    }

    @Test
    void transactionDecoratorSignalsExhaustionExplicitly()
            throws Exception {

        String source = Files.readString(
                ROOT.resolve(
                        "infrastructure/persistence/transaction/"
                                + "TransactionalObserveCustomerUseCase.java"
                )
        );

        for (String required : List.of(
                "classifier.classify(",
                "retryPolicy.shouldRetry(",
                "retryPolicy.beforeRetry(",
                "ObservedCustomerProjectionRetryExhaustedException",
                "attempt <= retryPolicy.maxAttempts()"
        )) {
            assertTrue(
                    source.contains(required),
                    () -> "Missing retry orchestration: " + required
            );
        }

        assertFalse(source.contains("while (true)"));
        assertFalse(source.contains("Thread.sleep("));
    }

    private static void assertNoTokens(
            Path root,
            List<String> forbidden
    ) throws Exception {

        try (var paths = Files.walk(root)) {
            List<String> violations = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .flatMap(path -> {
                        try {
                            String source = Files.readString(path);
                            return forbidden.stream()
                                    .filter(source::contains)
                                    .map(token ->
                                            path + " contains " + token
                                    );
                        } catch (Exception exception) {
                            throw new IllegalStateException(exception);
                        }
                    })
                    .toList();

            assertTrue(
                    violations.isEmpty(),
                    () -> "Resilience boundary violations: "
                            + violations
            );
        }
    }
}
