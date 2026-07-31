package com.sixpay.payment.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentArchitectureTest {

    private static final Path JAVA_ROOT =
            Path.of("src/main/java/com/sixpay/payment");

    private static final Path DOMAIN_ROOT =
            JAVA_ROOT.resolve("domain");

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

    private static final List<String> FORBIDDEN_LOT_3_PRODUCTION_LAYERS =
            List.of(
                    "api",
                    "application",
                    "infrastructure",
                    "configuration",
                    "events"
            );

    @Test
    void moduleContainsOnlyTheApprovedFoundationSources() {
        assertTrue(
                Files.isRegularFile(JAVA_ROOT.resolve("PaymentModule.java")),
                "PaymentModule marker is required"
        );
        assertTrue(
                Files.isRegularFile(DOMAIN_ROOT.resolve("package-info.java")),
                "The pure domain package must be established"
        );

        FORBIDDEN_LOT_3_PRODUCTION_LAYERS.forEach(layer ->
                assertFalse(
                        containsJavaSources(JAVA_ROOT.resolve(layer)),
                        "Lot 3 domain-only authorization forbids Java sources in "
                                + layer
                )
        );
    }

    @Test
    void moduleDeclaresOnlyFoundationDependencies() throws IOException {
        var pom = Files.readString(Path.of("pom.xml"));

        assertTrue(
                declaresArtifact(pom, "common"),
                "Payment directly uses common contracts"
        );
        assertTrue(
                declaresArtifact(pom, "shared-kernel"),
                "Payment must reuse the shared DDD primitives"
        );
        assertTrue(
                declaresArtifact(pom, "junit-jupiter"),
                "Payment foundation requires JUnit tests"
        );

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
                        "Payment foundation must not depend on " + artifact
                )
        );

        assertFalse(
                pom.contains("spring-boot-starter"),
                "The domain-only foundation must not introduce Spring starters"
        );
        assertFalse(
                pom.contains("<version>${project.version}</version>"),
                "Platform dependency versions must remain BOM-managed"
        );
    }

    @Test
    void domainRemainsFrameworkAgnostic() throws IOException {
        assertSourcesDoNotContain(
                DOMAIN_ROOT,
                FORBIDDEN_DOMAIN_IMPORTS
        );
    }

    @Test
    void paymentDoesNotDependOnAnotherBusinessDomain() throws IOException {
        assertSourcesDoNotContain(
                JAVA_ROOT,
                OTHER_BUSINESS_DOMAINS
        );
    }

    @Test
    void moduleIsNotAnExecutableSpringBootApplication() throws IOException {
        assertSourcesDoNotContain(
                JAVA_ROOT,
                List.of(
                        "@SpringBootApplication",
                        "public static void main("
                )
        );
    }

    @Test
    void controlledAuthorizationRemainsDomainOnly() throws IOException {
        var manifestPath = REPOSITORY_ROOT.resolve(
                "documentation/ai/payment/AI_CONTEXT_MANIFEST.yaml"
        );
        var manifest = Files.readString(manifestPath);

        assertTrue(
                manifest.contains("globalCodeGenerationAllowed: false"),
                "Global Payment code generation must remain disabled"
        );
        assertTrue(
                manifest.contains("scope: PAYMENT_DOMAIN_ONLY"),
                "The active authorization must be domain-only"
        );
        assertTrue(
                manifest.contains(
                        "currentIncrement: LOT_3_1_PAYMENT_MODULE_FOUNDATION"
                ),
                "Only Lot 3.1 must be activated"
        );
        assertTrue(
                manifest.contains(
                        "currentIncrementCodeGenerationAllowed: true"
                ),
                "Lot 3.1 requires an explicit positive authorization"
        );
        assertTrue(
                manifest.contains("futureIncrementActivationRequired: true"),
                "Every next Lot 3 increment requires explicit activation"
        );
        assertTrue(
                manifest.contains("APPLICATION_LAYER_GENERATION"),
                "Application generation must remain forbidden"
        );
        assertTrue(
                manifest.contains("PERSISTENCE_OR_MIGRATION_GENERATION"),
                "Persistence generation must remain forbidden"
        );
        assertTrue(
                manifest.contains("API_OR_CONTRACT_GENERATION"),
                "API and contract generation must remain forbidden"
        );
    }

    @Test
    void moduleDoesNotRedefineSharedPlatformPrimitives() {
        assertFalse(
                Files.exists(DOMAIN_ROOT.resolve("Money.java")),
                "Payment must reuse shared-kernel Money"
        );
        assertFalse(
                Files.exists(DOMAIN_ROOT.resolve("CorrelationId.java")),
                "Payment must reuse common CorrelationId"
        );
        assertFalse(
                Files.exists(DOMAIN_ROOT.resolve("AggregateRoot.java")),
                "Payment must reuse shared-kernel AggregateRoot"
        );
        assertFalse(
                Files.exists(DOMAIN_ROOT.resolve("DomainEvent.java")),
                "Payment must reuse shared-kernel DomainEvent"
        );
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
