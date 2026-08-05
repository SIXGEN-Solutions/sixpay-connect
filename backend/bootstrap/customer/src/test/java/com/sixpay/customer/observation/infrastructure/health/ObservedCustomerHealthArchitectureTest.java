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
        assertTrue(Files.isDirectory(ROOT));

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
                    .filter(path -> path.toString().endsWith(".java"))
                    .flatMap(path -> {
                        try {
                            String source = Files.readString(path);
                            return forbidden.stream()
                                    .filter(source::contains)
                                    .map(token -> path + " contains " + token);
                        } catch (Exception exception) {
                            throw new IllegalStateException(exception);
                        }
                    })
                    .toList();

            assertTrue(
                    violations.isEmpty(),
                    () -> "Unsafe health details: " + violations
            );
        }
    }

    @Test
    void indicatorsExposeRequiredOperationalConcepts()
            throws Exception {
        String projection = Files.readString(ROOT.resolve(
                "ObservedCustomerProjectionHealthIndicator.java"
        ));
        String query = Files.readString(ROOT.resolve(
                "ObservedCustomerQueryHealthIndicator.java"
        ));
        String audit = Files.readString(ROOT.resolve(
                "ObservedCustomerAuditHealthIndicator.java"
        ));

        for (String concept : List.of(
                "lastProcessedEventAt",
                "projectionLagMs",
                "retryExhaustedCount",
                "deadLetterCount"
        )) {
            assertTrue(projection.contains(concept));
        }

        for (String concept : List.of(
                "databaseReachable",
                "oldestProjectionAgeMs",
                "queryFailureRate"
        )) {
            assertTrue(query.contains(concept));
        }

        for (String concept : List.of(
                "repositoryReachable",
                "lastAuditPersistedAt",
                "auditFailureCount"
        )) {
            assertTrue(audit.contains(concept));
        }

        assertTrue(projection.contains("Health.status("DEGRADED")"));
        assertTrue(query.contains("Health.status("DEGRADED")"));
        assertTrue(audit.contains("Health.status("DEGRADED")"));
        assertFalse(projection.contains(".withException("));
        assertFalse(query.contains(".withException("));
        assertFalse(audit.contains(".withException("));
    }
}
