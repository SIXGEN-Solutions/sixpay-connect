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

    private static final Set<String> LOT_3_2_MODEL_SOURCES = Set.of(
            "BankPostingReference.java",
            "DebtorAccountReference.java",
            "ExternalPaymentReference.java",
            "ExternalSubscriptionReference.java",
            "ExternalSystem.java",
            "FailureCategory.java",
            "FailureCode.java",
            "FailureStage.java",
            "FinancialInstitutionCode.java",
            "IdempotencyKey.java",
            "PaymentFailure.java",
            "PaymentId.java",
            "PaymentRequestIdentity.java",
            "PaymentSource.java",
            "PaymentStatus.java",
            "PaymentValueObjectRules.java",
            "PublicPaymentReference.java",
            "RequestFingerprint.java",
            "RetryDisposition.java",
            "TreasuryAccountReference.java",
            "TreasuryAllocation.java",
            "TreasuryAllocationIntent.java",
            "TreasuryBeneficiaryReference.java",
            "package-info.java"
    );

    @Test
    void moduleContainsOnlyApprovedDomainLayers() {
        assertTrue(
                Files.isRegularFile(JAVA_ROOT.resolve("PaymentModule.java")),
                "PaymentModule marker is required"
        );
        assertTrue(
                Files.isRegularFile(DOMAIN_ROOT.resolve("package-info.java")),
                "The pure domain package must exist"
        );

        FORBIDDEN_PRODUCTION_LAYERS.forEach(layer ->
                assertFalse(
                        containsJavaSources(JAVA_ROOT.resolve(layer)),
                        "Domain-only authorization forbids Java sources in "
                                + layer
                )
        );
    }

    @Test
    void lot32ImplementsExactlyTheAuthorizedModelSources()
            throws IOException {
        try (Stream<Path> paths = Files.list(MODEL_ROOT)) {
            Set<String> actual = paths
                    .filter(path ->
                            path.toString().endsWith(".java")
                    )
                    .map(path -> path.getFileName().toString())
                    .collect(Collectors.toSet());

            assertEquals(
                    LOT_3_2_MODEL_SOURCES,
                    actual
            );
        }

        List.of(
                "Payment.java",
                "PaymentState.java",
                "PostingInstructionIdentity.java",
                "ReversalInstructionIdentity.java",
                "EvidenceMetadata.java"
        ).forEach(filename ->
                assertFalse(
                        Files.exists(MODEL_ROOT.resolve(filename)),
                        filename + " belongs to a later increment"
                )
        );
    }

    @Test
    void moduleDeclaresOnlyFoundationDependencies() throws IOException {
        var pom = Files.readString(Path.of("pom.xml"));

        assertTrue(declaresArtifact(pom, "common"));
        assertTrue(declaresArtifact(pom, "shared-kernel"));
        assertTrue(declaresArtifact(pom, "junit-jupiter"));

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
                assertFalse(
                        declaresArtifact(pom, artifact),
                        "Payment must not depend on " + artifact
                )
        );

        assertFalse(pom.contains("spring-boot-starter"));
    }

    @Test
    void domainRemainsFrameworkAgnostic() throws IOException {
        assertSourcesDoNotContain(
                DOMAIN_ROOT,
                FORBIDDEN_DOMAIN_IMPORTS
        );
    }

    @Test
    void paymentDoesNotDependOnAnotherBusinessDomain()
            throws IOException {
        assertSourcesDoNotContain(
                JAVA_ROOT,
                OTHER_BUSINESS_DOMAINS
        );
    }

    @Test
    void moduleIsNotAnExecutableSpringBootApplication()
            throws IOException {
        assertSourcesDoNotContain(
                JAVA_ROOT,
                List.of(
                        "@SpringBootApplication",
                        "public static void main("
                )
        );
    }

    @Test
    void controlledAuthorizationActivatesOnlyLot32()
            throws IOException {
        var manifestPath = REPOSITORY_ROOT.resolve(
                "documentation/ai/payment/AI_CONTEXT_MANIFEST.yaml"
        );
        var manifest = Files.readString(manifestPath);

        assertTrue(
                manifest.contains("globalCodeGenerationAllowed: false")
        );
        assertTrue(
                manifest.contains("scope: PAYMENT_DOMAIN_ONLY")
        );
        assertTrue(
                manifest.contains(
                        "currentIncrement: LOT_3_2_IDENTIFIERS_VALUE_OBJECTS"
                )
        );
        assertTrue(
                manifest.contains(
                        "currentIncrementCodeGenerationAllowed: true"
                )
        );
        assertTrue(
                manifest.contains("futureIncrementActivationRequired: true")
        );
        assertTrue(
                manifest.contains("SNAPSHOT_OR_EVIDENCE_GENERATION")
        );
        assertTrue(
                manifest.contains("AGGREGATE_ROOT_GENERATION")
        );
        assertTrue(
                manifest.contains("APPLICATION_LAYER_GENERATION")
        );
    }

    @Test
    void moduleReusesSharedPlatformPrimitives() {
        assertFalse(Files.exists(MODEL_ROOT.resolve("Money.java")));
        assertFalse(Files.exists(MODEL_ROOT.resolve("CorrelationId.java")));
        assertFalse(Files.exists(DOMAIN_ROOT.resolve("AggregateRoot.java")));
        assertFalse(Files.exists(DOMAIN_ROOT.resolve("DomainEvent.java")));
        assertFalse(Files.exists(DOMAIN_ROOT.resolve("ValueObject.java")));
    }

    @Test
    void protectedReferencesOverrideDefaultRecordRepresentation()
            throws IOException {
        String debtor = Files.readString(
                MODEL_ROOT.resolve("DebtorAccountReference.java")
        );
        String treasury = Files.readString(
                MODEL_ROOT.resolve("TreasuryAccountReference.java")
        );

        assertTrue(debtor.contains("public String toString()"));
        assertTrue(treasury.contains("public String toString()"));
        assertFalse(debtor.contains("record DebtorAccountReference"));
        assertFalse(treasury.contains("record TreasuryAccountReference"));
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
