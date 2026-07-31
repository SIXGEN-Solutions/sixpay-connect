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

    private static final Path EVIDENCE_ROOT =
            DOMAIN_ROOT.resolve("model/evidence");

    private static final Path REPOSITORY_ROOT =
            Path.of("..", "..").normalize();

    private static final List<String> FORBIDDEN_DOMAIN_IMPORTS = List.of(
            "import org.springframework.",
            "import jakarta.persistence.",
            "import jakarta.servlet.",
            "import org.hibernate.",
            "import tools.jackson.",
            "import com.sixpay.payment.api.",
            "import com.sixpay.payment.application.",
            "import com.sixpay.payment.infrastructure.",
            "import com.sixpay.payment.configuration.",
            "import com.sixpay.payment.events."
    );

    private static final List<String> OTHER_BUSINESS_DOMAINS = List.of(
            "import com.sixpay.customer.",
            "import com.sixpay.partner.",
            "import com.sixpay.subscription.",
            "import com.sixpay.accounting.",
            "import com.sixpay.reporting.",
            "import com.sixpay.notification.",
            "import com.sixpay.administration."
    );

    private static final List<String> FORBIDDEN_PRODUCTION_LAYERS =
            List.of(
                    "api",
                    "application",
                    "infrastructure",
                    "configuration",
                    "events"
            );

    private static final Set<String> LOT_3_3_EVIDENCE_SOURCES = Set.of(
            "AuthorizationBindingEvidence.java",
            "AuthorizationBindingResult.java",
            "AuthorizationBindingType.java",
            "AuthorizationDecisionOutcome.java",
            "AuthorizationEvidenceReference.java",
            "AuthorizationEvidenceSnapshot.java",
            "BankingVerificationCheckEvidence.java",
            "BankingVerificationCheckType.java",
            "BankingVerificationId.java",
            "BankingVerificationOutcome.java",
            "BankingVerificationSnapshot.java",
            "EndOfDayConfirmationSnapshot.java",
            "EvidenceCheckResult.java",
            "EvidenceFingerprint.java",
            "EvidenceMetadata.java",
            "EvidenceObservationChannel.java",
            "EvidenceValueObjectRules.java",
            "FundsControlCheckEvidence.java",
            "FundsControlCheckType.java",
            "FundsControlOutcome.java",
            "FundsControlSnapshot.java",
            "FundsVerificationReference.java",
            "PostingIdempotencyKey.java",
            "PostingInstructionId.java",
            "PostingLegEvidence.java",
            "PostingLegStatus.java",
            "PostingNextAction.java",
            "PostingOutcome.java",
            "PostingOutcomeSnapshot.java",
            "ReversalAuthorizationEvidence.java",
            "ReversalAuthorizationReference.java",
            "ReversalAuthorizationType.java",
            "ReversalIdempotencyKey.java",
            "ReversalInstructionId.java",
            "ReversalOutcome.java",
            "ReversalOutcomeEvidence.java",
            "ReversalReference.java",
            "ReversalSnapshot.java",
            "TfjConfirmationId.java",
            "TfjFailureEvidence.java",
            "TfjRecoveryAction.java",
            "TfjStatus.java",
            "TreasuryAccountResolutionSnapshot.java",
            "TreasuryResolutionOutcome.java",
            "package-info.java"
    );

    @Test
    void moduleContainsOnlyApprovedDomainLayers() {
        assertTrue(
                Files.isRegularFile(JAVA_ROOT.resolve("PaymentModule.java"))
        );

        FORBIDDEN_PRODUCTION_LAYERS.forEach(layer ->
                assertFalse(
                        containsJavaSources(JAVA_ROOT.resolve(layer)),
                        "Domain-only authorization forbids " + layer
                )
        );
    }

    @Test
    void lot33ImplementsExactlyTheAuthorizedEvidenceSources()
            throws IOException {
        try (Stream<Path> paths = Files.list(EVIDENCE_ROOT)) {
            Set<String> actual = paths
                    .filter(path -> path.toString().endsWith(".java"))
                    .map(path -> path.getFileName().toString())
                    .collect(Collectors.toSet());

            assertEquals(LOT_3_3_EVIDENCE_SOURCES, actual);
        }
    }

    @Test
    void aggregatePoliciesServicesAndEventsRemainDeferred() {
        List.of("Payment.java", "PaymentState.java").forEach(filename ->
                assertFalse(
                        Files.exists(
                                DOMAIN_ROOT.resolve("model")
                                        .resolve(filename)
                        )
                )
        );

        assertFalse(containsJavaSources(DOMAIN_ROOT.resolve("policy")));
        assertFalse(containsJavaSources(DOMAIN_ROOT.resolve("service")));
        assertFalse(containsJavaSources(DOMAIN_ROOT.resolve("event")));
    }

    @Test
    void moduleDependenciesRemainUnchanged() throws IOException {
        var pom = Files.readString(Path.of("pom.xml"));

        assertTrue(declaresArtifact(pom, "common"));
        assertTrue(declaresArtifact(pom, "shared-kernel"));
        assertTrue(declaresArtifact(pom, "junit-jupiter"));
        assertFalse(pom.contains("spring-boot-starter"));

        List.of(
                "security",
                "integration",
                "customer",
                "partner",
                "subscription",
                "accounting",
                "reporting",
                "notification",
                "administration"
        ).forEach(artifact ->
                assertFalse(declaresArtifact(pom, artifact))
        );
    }

    @Test
    void domainRemainsFrameworkAndBusinessModuleAgnostic()
            throws IOException {
        assertSourcesDoNotContain(DOMAIN_ROOT, FORBIDDEN_DOMAIN_IMPORTS);
        assertSourcesDoNotContain(DOMAIN_ROOT, OTHER_BUSINESS_DOMAINS);
    }

    @Test
    void evidenceContainsNoIoClockOrCryptoExecution()
            throws IOException {
        assertSourcesDoNotContain(
                EVIDENCE_ROOT,
                List.of(
                        "java.net.",
                        "java.sql.",
                        "java.nio.file.",
                        "java.security.",
                        "javax.crypto.",
                        "Instant.now(",
                        "System.currentTimeMillis(",
                        "Repository",
                        "RestClient",
                        "WebClient",
                        "KafkaTemplate"
                )
        );
    }

    @Test
    void controlledAuthorizationActivatesOnlyLot33()
            throws IOException {
        var manifest = Files.readString(
                REPOSITORY_ROOT.resolve(
                        "documentation/ai/payment/AI_CONTEXT_MANIFEST.yaml"
                )
        );

        assertTrue(
                manifest.contains("globalCodeGenerationAllowed: false")
        );
        assertTrue(
                manifest.contains("scope: PAYMENT_DOMAIN_ONLY")
        );
        assertTrue(
                manifest.contains(
                        "currentIncrement: LOT_3_3_SNAPSHOTS_FINANCIAL_EVIDENCE"
                )
        );
        assertTrue(
                manifest.contains(
                        "currentIncrementCodeGenerationAllowed: true"
                )
        );
        assertTrue(
                manifest.contains("AGGREGATE_ROOT_GENERATION")
        );
        assertTrue(
                manifest.contains("POLICY_OR_DOMAIN_SERVICE_GENERATION")
        );
        assertTrue(
                manifest.contains("DOMAIN_EVENT_GENERATION")
        );
    }

    @Test
    void fullSnapshotsAreNotRecordsWithAutomaticStringExposure()
            throws IOException {
        for (String filename : List.of(
                "AuthorizationEvidenceSnapshot.java",
                "BankingVerificationSnapshot.java",
                "FundsControlSnapshot.java",
                "TreasuryAccountResolutionSnapshot.java",
                "PostingOutcomeSnapshot.java",
                "EndOfDayConfirmationSnapshot.java",
                "ReversalSnapshot.java"
        )) {
            String content = Files.readString(
                    EVIDENCE_ROOT.resolve(filename)
            );

            assertFalse(
                    content.contains(
                            "record " + filename.replace(".java", "")
                    )
            );
            assertTrue(content.contains("public String toString()"));
        }
    }

    private static boolean declaresArtifact(
            String pom,
            String artifactId
    ) {
        return pom.contains(
                "<artifactId>" + artifactId + "</artifactId>"
        );
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
            throw new IllegalStateException(
                    "cannot inspect " + root,
                    exception
            );
        }
    }

    private static void assertSourcesDoNotContain(
            Path root,
            List<String> forbiddenTokens
    ) throws IOException {
        if (!Files.isDirectory(root)) {
            return;
        }

        try (Stream<Path> paths = Files.walk(root)) {
            var violations = paths
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
            var content = Files.readString(path);

            return forbiddenTokens.stream()
                    .filter(content::contains)
                    .map(token ->
                            path + " contains forbidden token " + token
                    )
                    .toList();
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "cannot inspect " + path,
                    exception
            );
        }
    }
}
