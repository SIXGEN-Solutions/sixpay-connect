package com.sixpay.payment.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentArchitectureTest {

    private static final Path JAVA_ROOT =
            Path.of("src/main/java/com/sixpay/payment");
    private static final Path DOMAIN_ROOT =
            JAVA_ROOT.resolve("domain");
    private static final Path MODEL_ROOT =
            DOMAIN_ROOT.resolve("model");
    private static final Path EVENT_ROOT =
            DOMAIN_ROOT.resolve("event");
    private static final Path POLICY_ROOT =
            DOMAIN_ROOT.resolve("policy");
    private static final Path SERVICE_ROOT =
            DOMAIN_ROOT.resolve("service");
    private static final Path REPOSITORY_ROOT =
            Path.of("..", "..").normalize();

    private static final Set<String> EVENT_RECORD_SOURCES = Set.of(
            "PaymentReceived.java",
            "PaymentAuthorizationCheckingStarted.java",
            "PaymentAuthorizationDecisionRecorded.java",
            "PaymentBankingVerificationRequested.java",
            "PaymentRejected.java",
            "PaymentImmediateResultAvailable.java",
            "PaymentBankingVerificationRecorded.java",
            "PaymentFundsControlRequested.java",
            "PaymentProcessingDeferred.java",
            "PaymentFundsControlRecorded.java",
            "PaymentTreasuryAccountResolutionRequested.java",
            "PaymentTreasuryAccountResolutionRecorded.java",
            "PaymentApprovedForPosting.java",
            "PaymentPostingAuthorized.java",
            "PaymentPostingRequested.java",
            "PaymentPostingOutcomeRecorded.java",
            "PaymentEndOfDayTrackingRequested.java",
            "PaymentDebitConfirmed.java",
            "PaymentPostingOutcomeLookupRequested.java",
            "PaymentReversalRequired.java",
            "PaymentPostingOutcomeResolved.java",
            "PaymentEndOfDayConfirmationRecorded.java",
            "TreasuryIntegrationConfirmed.java",
            "PaymentFinalResultAvailable.java",
            "PaymentTreasuryReconciliationRequired.java",
            "PaymentReversalAuthorized.java",
            "PaymentReversalRequested.java",
            "PaymentReversalOutcomeRecorded.java",
            "PaymentReversalResultAvailable.java",
            "PaymentReversalOutcomeLookupRequested.java",
            "PaymentReversalOutcomeResolved.java",
            "PaymentFailedWithoutFinancialEffect.java",
            "PaymentReversed.java"
    );

    @Test
    void aggregateStateAndThirtyThreeEventsExist()
            throws IOException {
        assertTrue(Files.isRegularFile(MODEL_ROOT.resolve("Payment.java")));
        assertTrue(
                Files.isRegularFile(
                        MODEL_ROOT.resolve("PaymentState.java")
                )
        );
        assertTrue(
                Files.isRegularFile(
                        MODEL_ROOT.resolve("NewPaymentIntent.java")
                )
        );
        assertTrue(
                Files.isRegularFile(
                        MODEL_ROOT.resolve(
                                "PaymentDomainException.java"
                        )
                )
        );

        Set<String> actualEventRecords;
        try (Stream<Path> paths = Files.list(EVENT_ROOT)) {
            actualEventRecords = paths
                    .filter(path ->
                            EVENT_RECORD_SOURCES.contains(
                                    path.getFileName().toString()
                            )
                    )
                    .map(path -> path.getFileName().toString())
                    .collect(Collectors.toSet());
        }

        assertEquals(EVENT_RECORD_SOURCES, actualEventRecords);
        assertEquals(33, actualEventRecords.size());
    }

    @Test
    void fourteenPoliciesTwelveProfilesAndFourServicesRemain()
            throws IOException {
        assertEquals(
                14,
                countFilesEndingWith(POLICY_ROOT, "Policy.java")
        );
        assertEquals(
                12,
                countFilesEndingWith(POLICY_ROOT, "Profile.java")
        );
        assertEquals(
                4,
                countFilesEndingWith(SERVICE_ROOT, "Service.java")
        );
    }

    @Test
    void paymentDomainRemainsFrameworkAndIoFree()
            throws IOException {
        assertSourcesDoNotContain(
                DOMAIN_ROOT,
                List.of(
                        "import org.springframework.",
                        "import jakarta.persistence.",
                        "import jakarta.servlet.",
                        "import org.hibernate.",
                        "import java.net.",
                        "import java.sql.",
                        "import java.nio.file.",
                        "import java.security.",
                        "import javax.crypto.",
                        "Instant.now(",
                        "System.currentTimeMillis(",
                        "KafkaTemplate",
                        "WebClient",
                        "RestClient",
                        "JdbcTemplate",
                        "EntityManager"
                )
        );
    }

    @Test
    void aggregateHasNamedOperationsAndNoGenericMutationApi()
            throws IOException {
        String aggregate = Files.readString(
                MODEL_ROOT.resolve("Payment.java")
        );

        for (String operation : List.of(
                "receive(",
                "startAuthorizationChecking(",
                "recordAuthorizationDecision(",
                "recordBankingVerification(",
                "recordFundsControl(",
                "recordTreasuryAccountResolution(",
                "authorizePosting(",
                "recordPostingOutcome(",
                "resolvePostingOutcome(",
                "recordMatchedEndOfDayConfirmation(",
                "authorizeReversal(",
                "recordReversalOutcome(",
                "resolveReversalOutcome(",
                "reject(",
                "recordRecoverableFailure(",
                "failWithoutFinancialEffect(",
                "reconstitute("
        )) {
            assertTrue(
                    aggregate.contains(operation),
                    () -> "Missing aggregate operation " + operation
            );
        }

        assertFalse(aggregate.contains("setStatus("));
        assertFalse(aggregate.contains("applyCommand("));
        assertFalse(aggregate.contains("handleCommand("));
    }

    @Test
    void applicationPersistenceAndAdaptersRemainForbidden() {
        for (String layer : List.of(
                "api",
                "application",
                "configuration",
                "events",
                "infrastructure"
        )) {
            assertFalse(
                    containsJavaSources(JAVA_ROOT.resolve(layer)),
                    "Lot 3.5 forbids layer " + layer
            );
        }

        assertFalse(
                containsJavaSources(
                        Path.of("src/main/resources/db")
                )
        );
        assertFalse(
                containsJavaSources(
                        Path.of("src/main/resources/openapi")
                )
        );
    }

    @Test
    void lot35AuthorizationIsActiveAndGlobalGenerationIsFalse()
            throws IOException {
        String manifest = Files.readString(
                REPOSITORY_ROOT.resolve(
                        "documentation/ai/payment/AI_CONTEXT_MANIFEST.yaml"
                )
        );

        assertTrue(
                manifest.contains("globalCodeGenerationAllowed: false")
        );
        assertTrue(
                manifest.contains(
                        "currentIncrement: LOT_3_5_AGGREGATE_ROOT_DOMAIN_EVENTS"
                )
        );
        assertTrue(
                manifest.contains(
                        "currentIncrementCodeGenerationAllowed: true"
                )
        );
        assertTrue(
                manifest.contains(
                        "APPLICATION_HANDLER_GENERATION"
                )
        );
        assertTrue(
                manifest.contains(
                        "PERSISTENCE_OR_MIGRATION_GENERATION"
                )
        );
        assertTrue(
                manifest.contains("OUTBOX_OR_MESSAGING_GENERATION")
        );
    }

    private static int countFilesEndingWith(
            Path root,
            String suffix
    ) throws IOException {
        try (Stream<Path> paths = Files.list(root)) {
            return (int) paths
                    .filter(path ->
                            path.getFileName().toString()
                                    .endsWith(suffix)
                    )
                    .count();
        }
    }

    private static boolean containsJavaSources(Path root) {
        if (!Files.isDirectory(root)) {
            return false;
        }

        try (Stream<Path> paths = Files.walk(root)) {
            return paths.anyMatch(
                    path -> path.toString().endsWith(".java")
            );
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static void assertSourcesDoNotContain(
            Path root,
            List<String> forbiddenTokens
    ) throws IOException {
        try (Stream<Path> paths = Files.walk(root)) {
            List<String> violations = paths
                    .filter(path -> path.toString().endsWith(".java"))
                    .flatMap(path ->
                            violations(path, forbiddenTokens).stream()
                    )
                    .toList();

            assertTrue(
                    violations.isEmpty(),
                    () -> "Architecture violations: " + violations
            );
        }
    }

    private static List<String> violations(
            Path path,
            List<String> forbiddenTokens
    ) {
        try {
            String content = Files.readString(path);
            return forbiddenTokens.stream()
                    .filter(content::contains)
                    .map(token ->
                            path + " contains forbidden token " + token
                    )
                    .toList();
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
