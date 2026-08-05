package com.sixpay.customer.observation.application.architecture;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ObservedCustomerLot487ObservabilityArchitectureTest {

    private static final Path ROOT = Path.of(
            "src/main/java/com/sixpay/customer/observation"
    );

    @Test
    void projectionDefinesAllRequiredMetrics()
            throws Exception {

        String metrics = Files.readString(
                ROOT.resolve(
                        "infrastructure/observability/"
                                + "ObservedCustomerProjectionMetrics.java"
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
                    metrics.contains(metric),
                    () -> "Missing projection metric: " + metric
            );
        }
    }

    @Test
    void queryDefinesAllRequiredMetrics()
            throws Exception {

        String observation = Files.readString(
                ROOT.resolve(
                        "api/observability/"
                                + "ObservedCustomerQueryObservation.java"
                )
        );

        for (String metric : List.of(
                "sixpay.customer.observation.query.requests",
                "sixpay.customer.observation.query.duration",
                "sixpay.customer.observation.query.results",
                "sixpay.customer.observation.query.failures"
        )) {
            assertTrue(
                    observation.contains(metric),
                    () -> "Missing query metric: " + metric
            );
        }
    }

    @Test
    void metricsUseOnlyBoundedTagsAndLogsContainNoPayload()
            throws Exception {

        for (Path sourcePath : List.of(
                ROOT.resolve(
                        "infrastructure/observability/"
                                + "ObservedCustomerProjectionMetrics.java"
                ),
                ROOT.resolve(
                        "api/observability/"
                                + "ObservedCustomerQueryObservation.java"
                )
        )) {
            String source = Files.readString(sourcePath);

            for (String forbiddenTag : List.of(
                    ".tag(\"eventId\"",
                    ".tag(\"sourceEventId\"",
                    ".tag(\"paymentId\"",
                    ".tag(\"observedCustomerId\"",
                    ".tag(\"correlationId\"",
                    ".tag(\"niu\"",
                    ".tag(\"legalName\"",
                    ".tag(\"institutionCode\"",
                    ".tag(\"failureReasonCode\"",
                    ".tag(\"cursor\""
            )) {
                assertFalse(
                        source.contains(forbiddenTag),
                        () -> sourcePath
                                + " contains forbidden metric tag "
                                + forbiddenTag
                );
            }

            for (String forbiddenLog : List.of(
                    "payload={}",
                    "command={}",
                    "response={}",
                    "normalizedNiu={}",
                    "legalName={}",
                    "accountBindingFingerprint={}",
                    "maskedAccountReference={}"
            )) {
                assertFalse(
                        source.contains(forbiddenLog),
                        () -> sourcePath
                                + " contains sensitive log "
                                + forbiddenLog
                );
            }
        }
    }

    @Test
    void applicationAndDomainContainNoMetricsFramework()
            throws Exception {

        for (Path layer : List.of(
                ROOT.resolve("application"),
                ROOT.resolve("domain")
        )) {
            assertNoTokens(
                    layer,
                    List.of(
                            "io.micrometer.",
                            "MeterRegistry",
                            "Counter.builder(",
                            "Timer.builder(",
                            "org.slf4j."
                    )
            );
        }
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
                    () -> "Observability boundary violations: "
                            + violations
            );
        }
    }
}
