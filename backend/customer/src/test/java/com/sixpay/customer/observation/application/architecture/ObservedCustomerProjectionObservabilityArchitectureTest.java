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

class ObservedCustomerProjectionObservabilityArchitectureTest {

    private static final Path ROOT = Path.of(
            "src/main/java/com/sixpay/customer/observation/"
                    + "infrastructure/observability"
    );

    @Test
    void packageContainsOnlyApprovedProjectionObservabilityTypes()
            throws Exception {
        assertEquals(
                Set.of(
                        "ObservedCustomerProjectionObservation.java",
                        "ObservedCustomerProjectionMetrics.java",
                        "ObservedCustomerProjectionResultType.java",
                        "ObservedCustomerProjectionErrorType.java",
                        "package-info.java"
                ),
                javaFiles(ROOT)
        );
    }

    @Test
    void allRequiredMetricNamesAreDeclared()
            throws Exception {
        String source = Files.readString(
                ROOT.resolve(
                        "ObservedCustomerProjectionMetrics.java"
                )
        );

        for (String metric : List.of(
                "sixpay.customer.observation.projection.requests",
                "sixpay.customer.observation.projection.duration",
                "sixpay.customer.observation.projection.results",
                "sixpay.customer.observation.projection.failures",
                "sixpay.customer.observation.projection.retries",
                "sixpay.customer.observation.projection.replays",
                "sixpay.customer.observation.projection.stale",
                "sixpay.customer.observation.projection.lag"
        )) {
            assertTrue(
                    source.contains(metric),
                    () -> "Missing projection metric: " + metric
            );
        }
    }

    @Test
    void metricTagsAreBoundedAndContainNoBusinessIdentifiers()
            throws Exception {
        String source = Files.readString(
                ROOT.resolve(
                        "ObservedCustomerProjectionMetrics.java"
                )
        );

        for (String allowed : List.of(
                "\"result\"",
                "\"error_type\"",
                "\"event_type\"",
                "\"attempt_bucket\""
        )) {
            assertTrue(
                    source.contains(allowed),
                    () -> "Missing bounded tag: " + allowed
            );
        }

        for (String forbidden : List.of(
                ".tag(\"eventId\"",
                ".tag(\"sourceEventId\"",
                ".tag(\"paymentId\"",
                ".tag(\"observedCustomerId\"",
                ".tag(\"correlationId\"",
                ".tag(\"niu\"",
                ".tag(\"normalizedNiu\"",
                ".tag(\"institutionCode\"",
                ".tag(\"financialInstitutionCode\"",
                ".tag(\"failureReasonCode\"",
                ".tag(\"accountBindingFingerprint\""
        )) {
            assertFalse(
                    source.contains(forbidden),
                    () -> "Forbidden projection metric tag: "
                            + forbidden
            );
        }
    }

    @Test
    void logsUseOnlyApprovedOperationalFieldsAndNeverPayload()
            throws Exception {
        String source = Files.readString(
                ROOT.resolve(
                        "ObservedCustomerProjectionObservation.java"
                )
        );

        for (String allowed : List.of(
                "sourceEventId={}",
                "paymentId={}",
                "observedCustomerId={}",
                "correlationId={}",
                "result={}",
                "attempt={}",
                "durationMs={}",
                "lagMs={}"
        )) {
            assertTrue(
                    source.contains(allowed),
                    () -> "Missing safe projection log field: "
                            + allowed
            );
        }

        for (String forbidden : List.of(
                "normalizedNiu",
                "legalName",
                "phone",
                "email",
                "accountBindingFingerprint",
                "maskedAccountReference",
                "financialInstitutionCode",
                "failureReasonCode",
                "payload",
                "command={}",
                "resultObject",
                "exception.getMessage()",
                "exception.toString()"
        )) {
            assertFalse(
                    source.contains(forbidden),
                    () -> "Sensitive projection log concept: "
                            + forbidden
            );
        }
    }

    @Test
    void applicationAndDomainDoNotDependOnMicrometerOrLogging()
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
                                                "io.micrometer.",
                                                "org.slf4j.",
                                                "MeterRegistry",
                                                "LoggerFactory"
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
                        () -> "Projection observability leaked into "
                                + "application/domain: "
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
