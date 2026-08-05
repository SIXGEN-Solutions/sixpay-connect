package com.sixpay.customer.observation.application.architecture;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ObservedCustomerProjectionResilienceArchitectureTest {

    private static final Path ROOT = Path.of(
            "src/main/java/com/sixpay/customer/observation/"
                    + "infrastructure/resilience"
    );

    @Test
    void resiliencePackageContainsApprovedTypes()
            throws Exception {
        assertEquals(
                Set.of(
                        "ObservedCustomerProjectionFailureType.java",
                        "ObservedCustomerProjectionFailureClassifier.java",
                        "ObservedCustomerProjectionRetryPolicy.java",
                        "ObservedCustomerProjectionRetryExhaustedException.java",
                        "ObservedCustomerProjectionBackoff.java",
                        "LockSupportObservedCustomerProjectionBackoff.java",
                        "package-info.java"
                ),
                javaFiles(ROOT)
        );
    }

    @Test
    void retryIsExplicitBoundedAndInjected()
            throws Exception {
        String policy = Files.readString(
                ROOT.resolve(
                        "ObservedCustomerProjectionRetryPolicy.java"
                )
        );

        for (String required : List.of(
                "maxAttempts",
                "initialBackoff",
                "maxBackoff",
                "multiplier",
                "jitter",
                "ObservedCustomerProjectionBackoff",
                "DoubleSupplier",
                "shouldRetry(",
                "beforeRetry("
        )) {
            assertTrue(
                    policy.contains(required),
                    () -> "Missing retry policy concept: "
                            + required
            );
        }

        assertFalse(policy.contains("Thread.sleep("));
        assertFalse(policy.contains("Instant.now("));
        assertFalse(policy.contains("Math.random("));
    }

    @Test
    void classifierRecognizesRetryableAndPermanentClasses()
            throws Exception {
        String classifier = Files.readString(
                ROOT.resolve(
                        "ObservedCustomerProjectionFailureClassifier.java"
                )
        );

        for (String required : List.of(
                "ObjectOptimisticLockingFailureException",
                "OptimisticLockException",
                "40001",
                "40P01",
                "23505",
                "startsWith(\"08\")",
                "IDEMPOTENCE_RACE",
                "INVALID_PAYLOAD",
                "PERMANENT_CRYPTOGRAPHY",
                "INCOMPATIBLE_CONTRACT"
        )) {
            assertTrue(
                    classifier.contains(required),
                    () -> "Missing failure classification: "
                            + required
            );
        }
    }

    @Test
    void applicationAndDomainContainNoRetryFrameworkOrSleeping()
            throws Exception {
        for (Path layer : List.of(
                Path.of(
                        "src/main/java/com/sixpay/customer/"
                                + "observation/application"
                ),
                Path.of(
                        "src/main/java/com/sixpay/customer/"
                                + "observation/domain"
                )
        )) {
            try (Stream<Path> paths = Files.walk(layer)) {
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
                                                "Thread.sleep(",
                                                "LockSupport.",
                                                "RetryTemplate",
                                                "org.springframework.retry",
                                                "ObservedCustomerProjectionRetryPolicy"
                                        )
                                        .stream()
                                        .filter(source::contains)
                                        .map(token ->
                                                path
                                                        + " contains "
                                                        + token
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
                        () -> "Retry leaked into application/domain: "
                                + violations
                );
            }
        }
    }

    private static Set<String> javaFiles(
            Path root
    ) throws Exception {
        try (Stream<Path> paths = Files.list(root)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(path ->
                            path.toString().endsWith(".java")
                    )
                    .map(path ->
                            path.getFileName().toString()
                    )
                    .collect(Collectors.toSet());
        }
    }
}
