package com.sixpay.customer.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomerArchitectureTest {

    private static final Path JAVA_ROOT =
            Path.of("src/main/java/com/sixpay/customer");

    private static final List<String> CAPABILITIES =
            List.of("verification", "observation");

    private static final Set<String> REQUIRED_CAPABILITY_PACKAGES =
            Set.of(
                    "api",
                    "application",
                    "configuration",
                    "domain",
                    "events",
                    "infrastructure"
            );

    private static final List<String> FORBIDDEN_DOMAIN_TOKENS =
            List.of(
                    "import org.springframework.",
                    "import jakarta.persistence.",
                    "import jakarta.servlet.",
                    "import org.hibernate.",
                    "import tools.jackson.",
                    "import java.net.",
                    "import java.sql.",
                    "import com.sixpay.customer.verification.api.",
                    "import com.sixpay.customer.verification.application.",
                    "import com.sixpay.customer.verification.configuration.",
                    "import com.sixpay.customer.verification.infrastructure.",
                    "import com.sixpay.customer.observation.api.",
                    "import com.sixpay.customer.observation.application.",
                    "import com.sixpay.customer.observation.configuration.",
                    "import com.sixpay.customer.observation.infrastructure."
            );

    private static final List<String> FORBIDDEN_BUSINESS_DOMAIN_IMPORTS =
            List.of(
                    "import com.sixpay.partner.",
                    "import com.sixpay.subscription.",
                    "import com.sixpay.payment.",
                    "import com.sixpay.accounting.",
                    "import com.sixpay.reporting.",
                    "import com.sixpay.notification.",
                    "import com.sixpay.administration."
            );

    @Test
    void moduleMarkerIsPresentAndNonExecutable() throws IOException {
        Path marker = JAVA_ROOT.resolve("CustomerModule.java");

        assertTrue(Files.isRegularFile(marker));

        String source = Files.readString(marker);

        assertTrue(source.contains("public final class CustomerModule"));
        assertFalse(source.contains("@SpringBootApplication"));
        assertFalse(source.contains("public static void main("));
    }

    @Test
    void moduleAutoConfigurationIsDeclaredAndRegistered()
            throws IOException {

        Path configuration = JAVA_ROOT.resolve(
                "configuration/CustomerModuleConfiguration.java"
        );
        Path imports = Path.of(
                "src/main/resources/META-INF/spring/"
                        + "org.springframework.boot.autoconfigure."
                        + "AutoConfiguration.imports"
        );

        assertTrue(Files.isRegularFile(configuration));
        assertTrue(Files.isRegularFile(imports));

        String configurationSource = Files.readString(configuration);
        String importsSource = Files.readString(imports);

        assertTrue(configurationSource.contains("@AutoConfiguration"));
        assertTrue(configurationSource.contains(
                "@ComponentScan(basePackageClasses = CustomerModule.class)"
        ));
        assertTrue(importsSource.contains(
                "com.sixpay.customer.configuration."
                        + "CustomerModuleConfiguration"
        ));
    }

    @Test
    void moduleContainsTheTwoApprovedCapabilities() {
        for (String capability : CAPABILITIES) {
            assertTrue(
                    Files.isDirectory(JAVA_ROOT.resolve(capability)),
                    () -> "Missing Customer capability: " + capability
            );
        }
    }

    @Test
    void capabilitiesFollowGoldenModuleTopLevelPackages() throws IOException {
        for (String capability : CAPABILITIES) {
            Path capabilityRoot = JAVA_ROOT.resolve(capability);

            Set<String> actual =
                    directDirectoriesContainingJavaSources(capabilityRoot);

            assertTrue(
                    actual.containsAll(REQUIRED_CAPABILITY_PACKAGES),
                    () -> capability
                            + " is missing packages: "
                            + difference(REQUIRED_CAPABILITY_PACKAGES, actual)
            );

            assertTrue(
                    REQUIRED_CAPABILITY_PACKAGES.containsAll(actual),
                    () -> capability
                            + " contains unexpected packages: "
                            + difference(actual, REQUIRED_CAPABILITY_PACKAGES)
            );

            assertTrue(
                    Files.isDirectory(
                            capabilityRoot.resolve(
                                    "infrastructure/persistence"
                            )
                    ),
                    () -> capability
                            + " must expose infrastructure/persistence"
            );
        }
    }

    @Test
    void capabilityDomainsRemainFrameworkAgnostic() throws IOException {
        for (String capability : CAPABILITIES) {
            assertSourcesDoNotContain(
                    JAVA_ROOT.resolve(capability).resolve("domain"),
                    FORBIDDEN_DOMAIN_TOKENS
            );
        }
    }

    @Test
    void applicationsDoNotDependOnApiInfrastructureOrConfiguration()
            throws IOException {

        for (String capability : CAPABILITIES) {
            String prefix = "import com.sixpay.customer."
                    + capability + ".";

            assertSourcesDoNotContain(
                    JAVA_ROOT.resolve(capability).resolve("application"),
                    List.of(
                            prefix + "api.",
                            prefix + "infrastructure.",
                            prefix + "configuration."
                    )
            );
        }
    }

    @Test
    void infrastructureDoesNotDependOnApiOrConfiguration()
            throws IOException {

        for (String capability : CAPABILITIES) {
            String prefix = "import com.sixpay.customer."
                    + capability + ".";

            assertSourcesDoNotContain(
                    JAVA_ROOT.resolve(capability).resolve("infrastructure"),
                    List.of(
                            prefix + "api.",
                            prefix + "configuration."
                    )
            );
        }
    }

    @Test
    void capabilityDomainsDoNotDependOnEachOther() throws IOException {
        assertSourcesDoNotContain(
                JAVA_ROOT.resolve("verification/domain"),
                List.of("import com.sixpay.customer.observation.")
        );

        assertSourcesDoNotContain(
                JAVA_ROOT.resolve("observation/domain"),
                List.of("import com.sixpay.customer.verification.")
        );
    }

    @Test
    void customerDoesNotDependOnAnotherBusinessDomain()
            throws IOException {

        assertSourcesDoNotContain(
                JAVA_ROOT,
                FORBIDDEN_BUSINESS_DOMAIN_IMPORTS
        );
    }

    @Test
    void customerDoesNotDeclarePaymentOrIntegrationAsMavenDependencies()
            throws IOException {

        String pom = Files.readString(Path.of("pom.xml"));

        assertFalse(
                pom.contains("<artifactId>payment</artifactId>")
        );
        assertFalse(
                pom.contains("<artifactId>integration</artifactId>")
        );
    }

    @Test
    void moduleDeclaresTheGoldenPlatformAndTestFoundation()
            throws IOException {

        String pom = Files.readString(Path.of("pom.xml"));

        for (String artifactId : List.of(
                "common",
                "shared-kernel",
                "security",
                "spring-boot-starter-webmvc",
                "spring-boot-starter-validation",
                "spring-boot-starter-data-jpa",
                "spring-boot-starter-security",
                "spring-boot-starter-actuator",
                "postgresql",
                "springdoc-openapi-starter-webmvc-api",
                "spring-boot-starter-test",
                "spring-boot-starter-webmvc-test",
                "spring-boot-starter-security-test",
                "spring-boot-starter-flyway",
                "flyway-database-postgresql",
                "testcontainers-junit-jupiter",
                "testcontainers-postgresql"
        )) {
            assertTrue(
                    pom.contains(
                            "<artifactId>" + artifactId + "</artifactId>"
                    ),
                    () -> "Missing Customer dependency: " + artifactId
            );
        }
    }

    @Test
    void moduleIsNotAnExecutableSpringBootApplication()
            throws IOException {

        assertSourcesDoNotContain(
                JAVA_ROOT,
                List.of("@SpringBootApplication")
        );
    }

    private static Set<String> directDirectoriesContainingJavaSources(
            Path root
    ) throws IOException {

        if (!Files.isDirectory(root)) {
            return Set.of();
        }

        try (Stream<Path> paths = Files.list(root)) {
            return paths
                    .filter(Files::isDirectory)
                    .filter(CustomerArchitectureTest::containsJavaSources)
                    .map(path -> path.getFileName().toString())
                    .collect(Collectors.toSet());
        }
    }

    private static boolean containsJavaSources(Path root) {
        if (!Files.isDirectory(root)) {
            return false;
        }

        try (Stream<Path> paths = Files.walk(root)) {
            return paths.anyMatch(
                    path -> Files.isRegularFile(path)
                            && path.toString().endsWith(".java")
            );
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static void assertSourcesDoNotContain(
            Path root,
            List<String> forbiddenTokens
    ) throws IOException {

        if (!Files.isDirectory(root)) {
            return;
        }

        List<String> violations;

        try (Stream<Path> paths = Files.walk(root)) {
            violations = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .flatMap(path ->
                            violations(path, forbiddenTokens).stream()
                    )
                    .toList();
        }

        assertTrue(
                violations.isEmpty(),
                () -> "Architecture violations: " + violations
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
                    .map(token -> path + " contains forbidden token " + token)
                    .toList();
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Cannot inspect " + path,
                    exception
            );
        }
    }

    private static Set<String> difference(
            Set<String> left,
            Set<String> right
    ) {
        return left.stream()
                .filter(value -> !right.contains(value))
                .collect(Collectors.toSet());
    }
}
