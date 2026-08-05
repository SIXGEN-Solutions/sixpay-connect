package com.sixpay.customer.observation.infrastructure.health;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ObservedCustomerHealthArchitectureTest {

    private static final Path ROOT = Path.of(
            "src/main/java/com/sixpay/customer/observation/"
                    + "infrastructure/health"
    );

    @Test
    void healthIndicatorsExposeOnlySafeAggregateDetails()
            throws Exception {

        assertTrue(
                Files.isDirectory(ROOT),
                () -> "Missing health directory: "
                        + ROOT.toAbsolutePath()
        );

        List<String> forbidden = List.of(
                "observedCustomerId",
                "sourceEventId",
                "paymentId",
                "correlationId",
                "normalizedNiu",
                "legalName",
                "email",
                "phone",
                "accountBindingFingerprint",
                "payload",
                "exception.getMessage()",
                "exception.toString()",
                ".withException("
        );

        try (var paths = Files.walk(ROOT)) {
            List<String> violations = paths
                    .filter(Files::isRegularFile)
                    .filter(path ->
                            path.toString().endsWith(".java")
                    )
                    .filter(path ->
                            !path.getFileName()
                                    .toString()
                                    .equals("package-info.java")
                    )
                    .flatMap(path -> {
                        try {
                            String source =
                                    Files.readString(path);

                            String executableSource =
                                    removeComments(source);

                            return forbidden.stream()
                                    .filter(
                                            executableSource::contains
                                    )
                                    .map(token ->
                                            path
                                                    + " contains "
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
                    () -> "Unsafe health details: "
                            + violations
            );
        }
    }

    @Test
    void indicatorsExposeRequiredOperationalConcepts()
            throws Exception {

        String projection = Files.readString(
                ROOT.resolve(
                        "ObservedCustomerProjectionHealthIndicator.java"
                )
        );

        String query = Files.readString(
                ROOT.resolve(
                        "ObservedCustomerQueryHealthIndicator.java"
                )
        );

        String audit = Files.readString(
                ROOT.resolve(
                        "ObservedCustomerAuditHealthIndicator.java"
                )
        );

        for (String concept : List.of(
                "lastProcessedEventAt",
                "projectionLagMs",
                "retryExhaustedCount",
                "deadLetterCount"
        )) {
            assertTrue(
                    projection.contains(concept),
                    () -> "Missing projection health concept: "
                            + concept
            );
        }

        for (String concept : List.of(
                "databaseReachable",
                "oldestProjectionAgeMs",
                "queryFailureRate"
        )) {
            assertTrue(
                    query.contains(concept),
                    () -> "Missing query health concept: "
                            + concept
            );
        }

        for (String concept : List.of(
                "repositoryReachable",
                "lastAuditPersistedAt",
                "auditFailureCount"
        )) {
            assertTrue(
                    audit.contains(concept),
                    () -> "Missing audit health concept: "
                            + concept
            );
        }

        assertTrue(
                projection.contains(
                        "Health.status(\"DEGRADED\")"
                ),
                "Projection health must support DEGRADED"
        );

        assertTrue(
                query.contains(
                        "Health.status(\"DEGRADED\")"
                ),
                "Query health must support DEGRADED"
        );

        assertTrue(
                audit.contains(
                        "Health.status(\"DEGRADED\")"
                ),
                "Audit health must support DEGRADED"
        );

        assertFalse(
                projection.contains(".withException("),
                "Projection health must not expose exceptions"
        );

        assertFalse(
                query.contains(".withException("),
                "Query health must not expose exceptions"
        );

        assertFalse(
                audit.contains(".withException("),
                "Audit health must not expose exceptions"
        );
    }

    private static String removeComments(
            String source
    ) {
        String withoutBlockComments =
                source.replaceAll(
                        "(?s)/\\*.*?\\*/",
                        ""
                );

        return withoutBlockComments.replaceAll(
                "(?m)//.*$",
                ""
        );
    }
}