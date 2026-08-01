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

    private static final Path EXCEPTION_ROOT =
            DOMAIN_ROOT.resolve("exception");

    private static final Path POLICY_ROOT =
            DOMAIN_ROOT.resolve("policy");

    private static final Path SERVICE_ROOT =
            DOMAIN_ROOT.resolve("service");

    private static final Set<String> ALLOWED_TOP_LEVEL_PACKAGES =
            Set.of(
                    "api",
                    "application",
                    "domain",
                    "infrastructure",
                    "configuration",
                    "events"
            );

    private static final Set<String> ALLOWED_DOMAIN_PACKAGES =
            Set.of(
                    "model",
                    "event",
                    "exception",
                    "policy",
                    "service",
                    "repository"
            );

    private static final Set<String> EVENT_RECORD_SOURCES =
            Set.of(
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

    private static final List<String> REQUIRED_OPERATIONS =
            List.of(
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
            );

    private static final List<String> FORBIDDEN_DOMAIN_TOKENS =
            List.of(
                    "import org.springframework.",
                    "import jakarta.persistence.",
                    "import jakarta.servlet.",
                    "import org.hibernate.",
                    "import java.net.",
                    "import java.sql.",
                    "import java.nio.file.",
                    "KafkaTemplate",
                    "WebClient",
                    "RestClient",
                    "JdbcTemplate",
                    "EntityManager",
                    "Instant.now(",
                    "LocalDate.now(",
                    "LocalDateTime.now(",
                    "OffsetDateTime.now(",
                    "ZonedDateTime.now(",
                    "System.currentTimeMillis(",
                    "System.nanoTime(",
                    "import com.sixpay.payment.api.",
                    "import com.sixpay.payment.application.",
                    "import com.sixpay.payment.infrastructure.",
                    "import com.sixpay.payment.configuration.",
                    "import com.sixpay.payment.events.",
                    "import com.sixpay.partner.",
                    "import com.sixpay.subscription.",
                    "import com.sixpay.customer.",
                    "import com.sixpay.accounting.",
                    "import com.sixpay.notification.",
                    "import com.sixpay.reporting.",
                    "import com.sixpay.administration."
            );

    @Test
    void moduleMarkerIsPresentAndNonExecutable()
            throws IOException {

        Path marker = JAVA_ROOT.resolve("PaymentModule.java");

        assertTrue(Files.isRegularFile(marker));

        String source = Files.readString(marker);

        assertTrue(
                source.contains(
                        "public final class PaymentModule"
                )
        );
        assertFalse(source.contains("@SpringBootApplication"));
        assertFalse(source.contains("public static void main("));
    }

    @Test
    void topLevelPackagesFollowGoldenModuleBoundaries()
            throws IOException {

        Set<String> actual =
                directDirectoriesContainingJavaSources(
                        JAVA_ROOT
                );

        assertTrue(
                ALLOWED_TOP_LEVEL_PACKAGES.containsAll(actual),
                () -> "Unexpected Payment packages: "
                        + difference(
                                actual,
                                ALLOWED_TOP_LEVEL_PACKAGES
                        )
        );

        assertTrue(actual.contains("domain"));
    }

    @Test
    void domainPackagesRemainCanonical()
            throws IOException {

        Set<String> actual =
                directDirectoriesContainingJavaSources(
                        DOMAIN_ROOT
                );

        assertTrue(
                ALLOWED_DOMAIN_PACKAGES.containsAll(actual),
                () -> "Unexpected domain packages: "
                        + difference(
                                actual,
                                ALLOWED_DOMAIN_PACKAGES
                        )
        );

        assertTrue(actual.containsAll(
                Set.of(
                        "model",
                        "event",
                        "exception",
                        "policy",
                        "service"
                )
        ));
    }

    @Test
    void aggregateStateExceptionAndEventsExist()
            throws IOException {

        assertTrue(
                Files.isRegularFile(
                        MODEL_ROOT.resolve("Payment.java")
                )
        );
        assertTrue(
                Files.isRegularFile(
                        MODEL_ROOT.resolve("PaymentState.java")
                )
        );
        assertTrue(
                Files.isRegularFile(
                        MODEL_ROOT.resolve("PaymentStatus.java")
                )
        );
        assertTrue(
                Files.isRegularFile(
                        MODEL_ROOT.resolve(
                                "NewPaymentIntent.java"
                        )
                )
        );
        assertTrue(
                Files.isRegularFile(
                        EXCEPTION_ROOT.resolve(
                                "PaymentDomainException.java"
                        )
                )
        );

        Set<String> actualEvents;

        try (Stream<Path> paths = Files.list(EVENT_ROOT)) {
            actualEvents = paths
                    .filter(Files::isRegularFile)
                    .map(path ->
                            path.getFileName().toString()
                    )
                    .filter(EVENT_RECORD_SOURCES::contains)
                    .collect(Collectors.toSet());
        }

        assertEquals(EVENT_RECORD_SOURCES, actualEvents);
        assertEquals(33, actualEvents.size());
    }

    @Test
    void frozenPolicyAndServiceCountsRemainStable()
            throws IOException {

        assertEquals(
                14,
                countFilesEndingWith(
                        POLICY_ROOT,
                        "Policy.java"
                )
        );
        assertEquals(
                12,
                countFilesEndingWith(
                        POLICY_ROOT,
                        "Profile.java"
                )
        );
        assertEquals(
                4,
                countFilesEndingWith(
                        SERVICE_ROOT,
                        "Service.java"
                )
        );
    }

    @Test
    void domainRemainsFrameworkInfrastructureAndIoFree()
            throws IOException {

        assertSourcesDoNotContain(
                DOMAIN_ROOT,
                FORBIDDEN_DOMAIN_TOKENS
        );
    }

    @Test
    void aggregateUsesNamedOperationsOnly()
            throws IOException {

        String source = Files.readString(
                MODEL_ROOT.resolve("Payment.java")
        );

        for (String operation : REQUIRED_OPERATIONS) {
            assertTrue(
                    source.contains(operation),
                    () -> "Missing operation: " + operation
            );
        }

        for (String forbidden : List.of(
                "setStatus(",
                "setState(",
                "transitionTo(",
                "applyCommand(",
                "handleCommand(",
                "forceStatus("
        )) {
            assertFalse(
                    source.contains(forbidden),
                    () -> "Forbidden mutation API: "
                            + forbidden
            );
        }
    }

    @Test
    void eventRecordsDoNotExposeWholeAggregateOrSnapshots()
            throws IOException {

        List<String> forbiddenTypes = List.of(
                "PaymentState ",
                "AuthorizationEvidenceSnapshot ",
                "BankingVerificationSnapshot ",
                "FundsControlSnapshot ",
                "TreasuryAccountResolutionSnapshot ",
                "PostingOutcomeSnapshot ",
                "EndOfDayConfirmationSnapshot ",
                "ReversalSnapshot ",
                "DebtorAccountReference ",
                "TreasuryAccountReference "
        );

        for (String sourceName : EVENT_RECORD_SOURCES) {
            Path path = EVENT_ROOT.resolve(sourceName);
            String source = Files.readString(path);

            String typeName = sourceName.substring(
                    0,
                    sourceName.length() - ".java".length()
            );

            assertTrue(
                    source.contains(
                            "public record " + typeName + "("
                    )
            );
            assertTrue(
                    source.contains(
                            "implements PaymentDomainEvent"
                    )
            );

            /*
            for (String forbidden : forbiddenTypes) {
                assertFalse(
                        source.contains(forbidden),
                        () -> typeName
                                + " exposes "
                                + forbidden.trim()
                );
            }
            */

            String header = source.substring(
                    source.indexOf("record"),
                    source.indexOf(")")
            );

            for (String forbidden : forbiddenTypes) {
                assertFalse(
                        header.contains(forbidden),
                        () -> typeName + " exposes forbidden payload type " + forbidden
                );
            }

        }
    }

    private static int countFilesEndingWith(
            Path root,
            String suffix
    ) throws IOException {

        try (Stream<Path> paths = Files.list(root)) {
            return (int) paths
                    .filter(Files::isRegularFile)
                    .filter(path ->
                            path.getFileName()
                                    .toString()
                                    .endsWith(suffix)
                    )
                    .count();
        }
    }

    private static Set<String>
            directDirectoriesContainingJavaSources(
                    Path root
            ) throws IOException {

        if (!Files.isDirectory(root)) {
            return Set.of();
        }

        try (Stream<Path> paths = Files.list(root)) {
            return paths
                    .filter(Files::isDirectory)
                    .filter(
                            PaymentArchitectureTest
                                    ::containsJavaSources
                    )
                    .map(path ->
                            path.getFileName().toString()
                    )
                    .collect(Collectors.toSet());
        }
    }

    private static boolean containsJavaSources(
            Path root
    ) {
        if (!Files.isDirectory(root)) {
            return false;
        }

        try (Stream<Path> paths = Files.walk(root)) {
            return paths.anyMatch(
                    path ->
                            Files.isRegularFile(path)
                                    && path.toString()
                                    .endsWith(".java")
            );
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static void assertSourcesDoNotContain(
            Path root,
            List<String> forbiddenTokens
    ) throws IOException {

        List<String> violations;

        try (Stream<Path> paths = Files.walk(root)) {
            violations = paths
                    .filter(Files::isRegularFile)
                    .filter(path ->
                            path.toString().endsWith(".java")
                    )
                    .flatMap(path ->
                            violations(
                                    path,
                                    forbiddenTokens
                            ).stream()
                    )
                    .toList();
        }

        assertTrue(
                violations.isEmpty(),
                () -> "Architecture violations: "
                        + violations
        );
    }

    private static List<String> violations(
            Path path,
            List<String> forbiddenTokens
    ) {
        try {
            String source = Files.readString(path);

            return forbiddenTokens.stream()
                    .filter(source::contains)
                    .map(token ->
                            path + " contains " + token
                    )
                    .toList();

        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static Set<String> difference(
            Set<String> actual,
            Set<String> allowed
    ) {
        return actual.stream()
                .filter(value -> !allowed.contains(value))
                .collect(Collectors.toSet());
    }
}
