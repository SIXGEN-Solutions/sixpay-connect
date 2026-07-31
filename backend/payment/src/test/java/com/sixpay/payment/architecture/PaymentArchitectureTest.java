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

    private static final Path DOMAIN_ROOT =
            Path.of("src/main/java/com/sixpay/payment/domain");
    private static final Path POLICY_ROOT =
            DOMAIN_ROOT.resolve("policy");
    private static final Path SERVICE_ROOT =
            DOMAIN_ROOT.resolve("service");
    private static final Path REPOSITORY_ROOT =
            Path.of("..", "..").normalize();

    private static final Set<String> POLICY_SOURCES = Set.of(
            "AuthorizationEvidenceAcceptancePolicy.java",
            "AuthorizationPolicyProfile.java",
            "BankingVerificationAcceptancePolicy.java",
            "BankingVerificationPolicyProfile.java",
            "CurrentPostingEvidence.java",
            "CurrentReversalEvidence.java",
            "CurrentTfjEvidence.java",
            "EndOfDayConfirmationAcceptancePolicy.java",
            "EndOfDayDecision.java",
            "EndOfDayInterpretation.java",
            "EventDataClassification.java",
            "EventDisclosureDecision.java",
            "EventDisclosureProfile.java",
            "EvidenceAcceptanceDecision.java",
            "EvidenceAuthority.java",
            "EvidenceCategory.java",
            "EvidenceConclusiveness.java",
            "EvidenceIdentity.java",
            "EvidenceReplayDecision.java",
            "EvidenceReplayReplacementPolicy.java",
            "EvidenceTemporalDecision.java",
            "EvidenceTemporalProfile.java",
            "EvidenceTemporalValidityPolicy.java",
            "ExplicitEventPayload.java",
            "FailureClassificationPolicy.java",
            "FailureClassificationProfile.java",
            "FailureDispositionDecision.java",
            "FinancialEffectKnowledge.java",
            "FinancialOutcomePolicyProfile.java",
            "FundsControlAcceptancePolicy.java",
            "FundsControlPolicyProfile.java",
            "PaymentAuthorizationContext.java",
            "PaymentBankingContext.java",
            "PaymentEventDisclosurePolicy.java",
            "PaymentFundsContext.java",
            "PaymentLifecycleContext.java",
            "PaymentPostingAuthorizationContext.java",
            "PaymentPostingContext.java",
            "PaymentResultContext.java",
            "PaymentResultIntentPolicy.java",
            "PaymentReversalContext.java",
            "PaymentReversalEligibilityContext.java",
            "PaymentTfjContext.java",
            "PaymentTreasuryContext.java",
            "PolicyDecision.java",
            "PolicyProfileMetadata.java",
            "PostingAuthorizationDecision.java",
            "PostingAuthorizationPolicyProfile.java",
            "PostingDecision.java",
            "PostingInstructionAuthorizationPolicy.java",
            "PostingInstructionIdentity.java",
            "PostingOutcomeInterpretation.java",
            "PostingOutcomeInterpretationPolicy.java",
            "ResultIntentDecision.java",
            "ResultIntentPolicyProfile.java",
            "ReversalAuthorizationDecision.java",
            "ReversalAuthorizationPolicy.java",
            "ReversalDecision.java",
            "ReversalInstructionIdentity.java",
            "ReversalOutcomeInterpretation.java",
            "ReversalOutcomeInterpretationPolicy.java",
            "ReversalPolicyProfile.java",
            "TfjPolicyProfile.java",
            "TreasuryResolutionAcceptancePolicy.java",
            "TreasuryResolutionPolicyProfile.java",
            "UniqueTfjMatchProof.java",
            "package-info.java"
    );

    private static final Set<String> SERVICE_SOURCES = Set.of(
            "EndOfDayDecisionInput.java",
            "EndOfDayDecisionService.java",
            "PaymentPolicyBundle.java",
            "PaymentResultIntentService.java",
            "PostingDecisionInput.java",
            "PostingOutcomeDecisionService.java",
            "ReversalDecisionInput.java",
            "ReversalDecisionService.java",
            "package-info.java"
    );

    @Test
    void lot34ContainsExactlyAuthorizedPolicyAndServiceSources()
            throws IOException {
        assertEquals(POLICY_SOURCES, filenames(POLICY_ROOT));
        assertEquals(SERVICE_SOURCES, filenames(SERVICE_ROOT));
    }

    @Test
    void fourteenPoliciesTwelveProfilesAndFourServicesExist() {
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
    void policiesAndServicesRemainPure() throws IOException {
        assertSourcesDoNotContain(
                DOMAIN_ROOT,
                List.of(
                        "org.springframework.",
                        "jakarta.persistence.",
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
                        "KafkaTemplate",
                        "registerEvent(",
                        "addDomainEvent("
                )
        );
    }

    @Test
    void noAggregateMutationOrEventPackageIsIntroduced() {
        assertFalse(
                Files.exists(
                        DOMAIN_ROOT.resolve("model/Payment.java")
                )
        );
        assertFalse(
                Files.exists(
                        DOMAIN_ROOT.resolve("model/PaymentState.java")
                )
        );
        assertFalse(
                Files.isDirectory(DOMAIN_ROOT.resolve("event"))
        );
    }

    @Test
    void currentAuthorizationIsLot34DomainOnly()
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
                        "currentIncrement: LOT_3_4_POLICIES_DOMAIN_SERVICES"
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
                manifest.contains("DOMAIN_EVENT_GENERATION")
        );
    }

    private static Set<String> filenames(Path root)
            throws IOException {
        try (Stream<Path> paths = Files.list(root)) {
            return paths
                    .filter(path -> path.toString().endsWith(".java"))
                    .map(path -> path.getFileName().toString())
                    .collect(Collectors.toSet());
        }
    }

    private static int countFilesEndingWith(
            Path root,
            String suffix
    ) {
        try (Stream<Path> paths = Files.list(root)) {
            return (int) paths
                    .filter(path ->
                            path.getFileName().toString()
                                    .endsWith(suffix)
                    )
                    .count();
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
                    .flatMap(path -> violations(path, forbiddenTokens).stream())
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
