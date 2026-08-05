package com.sixpay.bootstrap.architecture;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomerProjectionOutboxObservabilityArchitectureTest {

    private static final Path ROOT = Path.of(
            "src/main/java/com/sixpay/bootstrap/"
                    + "integration/customer/outbox"
    );

    @Test
    void schedulerIsBootstrapOwnedAndConfigurationControlled()
            throws Exception {

        String scheduler = Files.readString(
                ROOT.resolve(
                        "PaymentObservedCustomerOutboxScheduler.java"
                )
        );

        String configuration = Files.readString(
                ROOT.resolve(
                        "PaymentObservedCustomerOutboxConfiguration.java"
                )
        );

        assertTrue(
                scheduler.contains("@Scheduled("),
                "Scheduler must use @Scheduled"
        );

        assertTrue(
                scheduler.contains("polling-interval"),
                "Scheduler must use the configured polling interval"
        );

        assertTrue(
                scheduler.contains(
                        "dispatcher.dispatchAvailable("
                ),
                "Scheduler must delegate to the dispatcher"
        );

        assertTrue(
                configuration.contains(
                        "@ConditionalOnProperty("
                ),
                "Scheduler bean must be controlled by configuration"
        );

        assertTrue(
                configuration.contains(
                        "havingValue = \"true\""
                ),
                "Scheduler must only be enabled when the property "
                        + "is true"
        );

        for (String forbidden : List.of(
                "payload()",
                "normalizedNiu",
                "accountBindingFingerprint",
                "legalName",
                "customerName",
                "maskedAccountReference"
        )) {
            assertFalse(
                    scheduler.contains(forbidden),
                    () -> "Scheduler logs or handles sensitive "
                            + "concept: "
                            + forbidden
            );
        }
    }

    @Test
    void metricsUseCanonicalNamesAndOnlyBoundedTags()
            throws Exception {

        String source = Files.readString(
                ROOT.resolve(
                        "CustomerProjectionOutboxMetrics.java"
                )
        );

        /*
         * The metric names are assembled from PREFIX and bounded suffixes.
         * Do not require the complete concatenated metric name to appear
         * literally in the Java source.
         */
        assertTrue(
                source.contains(
                        "\"sixpay.payment.outbox."
                                + "customer_projection\""
                ),
                "Missing canonical customer projection metric prefix"
        );

        for (String requiredSuffix : List.of(
                "\".claimed\"",
                "\".published\"",
                "\".retried\"",
                "\".dead_lettered\"",
                "\".duration\"",
                "\".lag\""
        )) {
            assertTrue(
                    source.contains(requiredSuffix),
                    () -> "Missing metric suffix: "
                            + requiredSuffix
            );
        }

        for (String requiredTag : List.of(
                "\"event_type\"",
                "\"result\"",
                "\"error_type\""
        )) {
            assertTrue(
                    source.contains(requiredTag),
                    () -> "Missing bounded metric tag: "
                            + requiredTag
            );
        }

        for (String forbiddenTag : List.of(
                "\"eventId\"",
                "\"paymentId\"",
                "\"correlationId\"",
                "\"normalizedNiu\"",
                "\"accountBindingFingerprint\"",
                "\"legalName\"",
                "\"customerName\"",
                "\"maskedAccountReference\""
        )) {
            assertFalse(
                    source.contains(forbiddenTag),
                    () -> "Forbidden metric tag or sensitive field: "
                            + forbiddenTag
            );
        }
    }

    @Test
    void metricErrorTypesRemainBounded()
            throws Exception {

        String source = Files.readString(
                ROOT.resolve(
                        "CustomerProjectionOutboxMetrics.java"
                )
        );

        for (String boundedErrorType : List.of(
                "unknown_event_type",
                "unsupported_event_version",
                "invalid_event_payload",
                "projection_domain_conflict",
                "invalid_contract",
                "temporary_persistence_failure",
                "temporary_infrastructure_failure",
                "other",
                "none"
        )) {
            assertTrue(
                    source.contains(
                            "\"" + boundedErrorType + "\""
                    ),
                    () -> "Missing bounded error type: "
                            + boundedErrorType
            );
        }

        assertTrue(
                source.contains("normalizeErrorType("),
                "Metric error types must be normalized before "
                        + "being used as tags"
        );
    }

    @Test
    void yamlContainsNoCredentialOrSecret()
            throws Exception {

        String yaml = Files.readString(
                Path.of(
                        "src/main/resources/"
                                + "application-customer-"
                                + "projection-outbox.yml"
                )
        );

        for (String required : List.of(
                "enabled: true",
                "batch-size: 50",
                "polling-interval: 1s",
                "max-attempts: 10",
                "initial-backoff: 1s",
                "max-backoff: 5m",
                "processing-timeout: 2m"
        )) {
            assertTrue(
                    yaml.contains(required),
                    () -> "Missing YAML property: " + required
            );
        }

        String normalizedYaml =
                yaml.toLowerCase();

        for (String forbidden : List.of(
                "password:",
                "secret:",
                "token:",
                "api-key:",
                "apikey:",
                "private-key:",
                "client-secret:"
        )) {
            assertFalse(
                    normalizedYaml.contains(forbidden),
                    () -> "Secret-like YAML property found: "
                            + forbidden
            );
        }
    }

    @Test
    void healthIndicatorExposesOnlyOperationalBacklogDetails()
            throws Exception {

        String source = Files.readString(
                ROOT.resolve(
                        "CustomerProjectionOutboxHealthIndicator.java"
                )
        );

        for (String required : List.of(
                "countOutstandingByEventType(",
                "findOldestOutstandingOccurredAt(",
                "\"outstandingEvents\"",
                "\"oldestEventLagSeconds\"",
                "\"batchSize\"",
                "\"maxAttempts\""
        )) {
            assertTrue(
                    source.contains(required),
                    () -> "Missing health backlog concept: "
                            + required
            );
        }

        for (String forbidden : List.of(
                "normalizedNiu",
                "accountBindingFingerprint",
                "legalName",
                "customerName",
                "payload()",
                "correlationId"
        )) {
            assertFalse(
                    source.contains(forbidden),
                    () -> "Health indicator exposes sensitive "
                            + "concept: "
                            + forbidden
            );
        }
    }
}