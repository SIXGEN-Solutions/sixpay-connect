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

    private static final Path VERIFICATION_ROOT =
            JAVA_ROOT.resolve("verification");

    private static final Path OBSERVATION_ROOT =
            JAVA_ROOT.resolve("observation");

    private static final Path MANAGEMENT_ROOT =
            JAVA_ROOT.resolve("management");

    private static final Path VERIFICATION_DOMAIN =
            VERIFICATION_ROOT.resolve("domain");

    private static final Path OBSERVATION_DOMAIN =
            OBSERVATION_ROOT.resolve("domain");

    private static final Path MANAGEMENT_DOMAIN =
            MANAGEMENT_ROOT.resolve("domain");

    /*
     * Verification and Observation are the original Customer
     * capabilities and follow the complete golden capability
     * structure.
     */
    private static final List<String> LEGACY_CAPABILITIES =
            List.of(
                    "verification",
                    "observation"
            );

    /*
     * Customer Management was introduced later as a dedicated
     * capability.
     *
     * It exposes an explicit configuration package because its
     * framework-free application ports require environment-neutral
     * runtime wiring.
     *
     * An events package is not required until the capability owns
     * domain/integration events that justify it.
     */
    private static final Set<String>
            REQUIRED_MANAGEMENT_PACKAGES =
            Set.of(
                    "api",
                    "application",
                    "configuration",
                    "domain",
                    "infrastructure"
            );

    private static final Set<String>
            REQUIRED_LEGACY_CAPABILITY_PACKAGES =
            Set.of(
                    "api",
                    "application",
                    "configuration",
                    "domain",
                    "events",
                    "infrastructure"
            );

    private static final Set<String>
            ALLOWED_VERIFICATION_DOMAIN_PACKAGES =
            Set.of(
                    "model",
                    "event",
                    "exception",
                    "policy",
                    "service",
                    "repository"
            );

    private static final Set<String>
            ALLOWED_OBSERVATION_DOMAIN_PACKAGES =
            Set.of(
                    "model",
                    "exception",
                    "policy"
            );

    private static final Set<String>
            ALLOWED_MANAGEMENT_DOMAIN_PACKAGES =
            Set.of(
                    "model",
                    "exception",
                    "repository"
            );

    private static final List<String>
            FORBIDDEN_DOMAIN_TOKENS =
            List.of(
                    "import org.springframework.",
                    "import jakarta.persistence.",
                    "import jakarta.servlet.",
                    "import org.hibernate.",
                    "import tools.jackson.",
                    "import java.net.",
                    "import java.sql.",

                    /*
                     * Verification domain must not depend on
                     * outer layers.
                     */
                    "import com.sixpay.customer.verification.api.",
                    "import com.sixpay.customer.verification.application.",
                    "import com.sixpay.customer.verification.configuration.",
                    "import com.sixpay.customer.verification.infrastructure.",
                    "import com.sixpay.customer.verification.events.",

                    /*
                     * Observation domain must not depend on
                     * outer layers.
                     */
                    "import com.sixpay.customer.observation.api.",
                    "import com.sixpay.customer.observation.application.",
                    "import com.sixpay.customer.observation.configuration.",
                    "import com.sixpay.customer.observation.infrastructure.",
                    "import com.sixpay.customer.observation.events.",

                    /*
                     * Management domain must not depend on
                     * outer layers.
                     */
                    "import com.sixpay.customer.management.api.",
                    "import com.sixpay.customer.management.application.",
                    "import com.sixpay.customer.management.infrastructure.",
                    "import com.sixpay.customer.management.configuration.",

                    "import com.sixpay.payment.",

                    "RestClient",
                    "WebClient",
                    "HttpClient",
                    "KafkaTemplate",
                    "EntityManager",
                    "JdbcTemplate",

                    "@Entity",
                    "@MappedSuperclass",
                    "@Embeddable",
                    "@Repository",
                    "@Service",
                    "@Component"
            );

    private static final List<String>
            FORBIDDEN_CURRENT_TIME_TOKENS =
            List.of(
                    "Instant.now(",
                    "LocalDate.now(",
                    "LocalDateTime.now(",
                    "OffsetDateTime.now(",
                    "ZonedDateTime.now(",
                    "System.currentTimeMillis(",
                    "System.nanoTime("
            );

    private static final List<String>
            FORBIDDEN_BUSINESS_DOMAIN_IMPORTS =
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
    void moduleMarkerIsPresentAndNonExecutable()
            throws IOException {

        Path marker =
                JAVA_ROOT.resolve(
                        "CustomerModule.java"
                );

        assertTrue(
                Files.isRegularFile(marker),
                "CustomerModule marker must exist"
        );

        String source =
                Files.readString(marker);

        assertTrue(
                source.contains(
                        "public final class CustomerModule"
                )
        );

        assertFalse(
                source.contains(
                        "@SpringBootApplication"
                )
        );

        assertFalse(
                source.contains(
                        "public static void main("
                )
        );
    }

    @Test
    void moduleAutoConfigurationIsDeclaredAndRegistered()
            throws IOException {

        Path configuration =
                JAVA_ROOT.resolve(
                        "configuration/"
                                + "CustomerModuleConfiguration.java"
                );

        Path imports =
                Path.of(
                        "src/main/resources/META-INF/spring/"
                                + "org.springframework.boot."
                                + "autoconfigure."
                                + "AutoConfiguration.imports"
                );

        assertTrue(
                Files.isRegularFile(configuration),
                "CustomerModuleConfiguration must exist"
        );

        assertTrue(
                Files.isRegularFile(imports),
                "AutoConfiguration.imports must exist"
        );

        String configurationSource =
                Files.readString(configuration);

        String importsSource =
                Files.readString(imports);

        assertTrue(
                configurationSource.contains(
                        "@AutoConfiguration"
                ),
                "Customer module must declare "
                        + "@AutoConfiguration"
        );

        /*
         * Do not assert the exact formatting of the
         * annotation.
         *
         * The golden module uses a multi-line
         * @ComponentScan with exclusion filters.
         */
        assertTrue(
                configurationSource.contains(
                        "@ComponentScan("
                ),
                "Customer module must declare "
                        + "@ComponentScan"
        );

        assertTrue(
                configurationSource.contains(
                        "basePackageClasses = "
                                + "CustomerModule.class"
                ),
                "Customer component scan must be "
                        + "anchored on CustomerModule"
        );

        /*
         * JPA configuration must be explicit.
         *
         * @ComponentScan alone does not register Spring
         * Data repository interfaces.
         */
        assertTrue(
                configurationSource.contains(
                        "@EntityScan("
                ),
                "Customer module must explicitly "
                        + "scan JPA entities"
        );

        assertTrue(
                configurationSource.contains(
                        "@EnableJpaRepositories("
                ),
                "Customer module must explicitly "
                        + "scan Spring Data repositories"
        );

        /*
         * Customer Management persistence package anchor.
         */
        assertTrue(
                configurationSource.contains(
                        "CustomerJpaEntity.class"
                ),
                "Customer Management entities "
                        + "must be registered"
        );

        assertTrue(
                configurationSource.contains(
                        "CustomerSpringDataRepository.class"
                ),
                "Customer Management repositories "
                        + "must be registered"
        );

        /*
         * Audit lives input a sibling infrastructure package
         * and therefore requires its own package anchor.
         */
        assertTrue(
                configurationSource.contains(
                        "CustomerAuditJpaEntity.class"
                ),
                "Customer audit entities "
                        + "must be registered"
        );

        assertTrue(
                configurationSource.contains(
                        "CustomerAuditSpringDataRepository.class"
                ),
                "Customer audit repositories "
                        + "must be registered"
        );

        assertTrue(
                importsSource.contains(
                        "com.sixpay.customer.configuration."
                                + "CustomerModuleConfiguration"
                ),
                "CustomerModuleConfiguration must be "
                        + "registered input "
                        + "AutoConfiguration.imports"
        );
    }

    @Test
    void moduleContainsTheApprovedCapabilities() {

        for (String capability : List.of(
                "verification",
                "observation",
                "management"
        )) {
            assertTrue(
                    Files.isDirectory(
                            JAVA_ROOT.resolve(capability)
                    ),
                    () ->
                            "Missing Customer capability: "
                                    + capability
            );
        }
    }

    @Test
    void legacyCapabilitiesFollowGoldenModuleTopLevelPackages()
            throws IOException {

        for (String capability : LEGACY_CAPABILITIES) {

            Path capabilityRoot =
                    JAVA_ROOT.resolve(capability);

            Set<String> actual =
                    directDirectoriesContainingJavaSources(
                            capabilityRoot
                    );

            assertTrue(
                    actual.containsAll(
                            REQUIRED_LEGACY_CAPABILITY_PACKAGES
                    ),
                    () ->
                            capability
                                    + " is missing packages: "
                                    + difference(
                                    REQUIRED_LEGACY_CAPABILITY_PACKAGES,
                                    actual
                            )
            );

            assertTrue(
                    REQUIRED_LEGACY_CAPABILITY_PACKAGES
                            .containsAll(actual),
                    () ->
                            capability
                                    + " contains unexpected packages: "
                                    + difference(
                                    actual,
                                    REQUIRED_LEGACY_CAPABILITY_PACKAGES
                            )
            );

            assertTrue(
                    Files.isDirectory(
                            capabilityRoot.resolve(
                                    "infrastructure/persistence"
                            )
                    ),
                    () ->
                            capability
                                    + " must expose "
                                    + "infrastructure/persistence"
            );
        }
    }

    @Test
    void managementCapabilityUsesItsApprovedTopLevelPackages()
            throws IOException {

        Set<String> actual =
                directDirectoriesContainingJavaSources(
                        MANAGEMENT_ROOT
                );

        assertTrue(
                actual.containsAll(
                        REQUIRED_MANAGEMENT_PACKAGES
                ),
                () ->
                        "management is missing packages: "
                                + difference(
                                REQUIRED_MANAGEMENT_PACKAGES,
                                actual
                        )
        );

        assertTrue(
                REQUIRED_MANAGEMENT_PACKAGES
                        .containsAll(actual),
                () ->
                        "management contains unexpected packages: "
                                + difference(
                                actual,
                                REQUIRED_MANAGEMENT_PACKAGES
                        )
        );

        assertTrue(
                Files.isDirectory(
                        MANAGEMENT_ROOT.resolve(
                                "infrastructure/persistence"
                        )
                ),
                "management must expose "
                        + "infrastructure/persistence"
        );

        /*
         * CM-7 deliberately isolates audit persistence
         * from aggregate persistence, following the golden
         * Partner module style.
         */
        assertTrue(
                Files.isDirectory(
                        MANAGEMENT_ROOT.resolve(
                                "infrastructure/audit"
                        )
                ),
                "management must expose "
                        + "infrastructure/audit"
        );

        assertTrue(
                Files.isRegularFile(
                        MANAGEMENT_ROOT.resolve(
                                "configuration/"
                                        + "CustomerManagementApplicationConfiguration.java"
                        )
                ),
                "management must expose its environment-neutral "
                        + "application runtime configuration"
        );

        assertTrue(
                Files.isRegularFile(
                        MANAGEMENT_ROOT.resolve(
                                "configuration/"
                                        + "CustomerManagementApplicationConfiguration.java"
                        )
                ),
                "management must expose its environment-neutral "
                        + "application runtime configuration"
        );
    }

    @Test
    void verificationDomainUsesOnlyApprovedSubpackages()
            throws IOException {

        Set<String> actual =
                directDirectoriesContainingJavaSources(
                        VERIFICATION_DOMAIN
                );

        assertTrue(
                ALLOWED_VERIFICATION_DOMAIN_PACKAGES
                        .containsAll(actual),
                () ->
                        "Unexpected verification domain "
                                + "packages: "
                                + difference(
                                actual,
                                ALLOWED_VERIFICATION_DOMAIN_PACKAGES
                        )
        );

        for (String required : List.of(
                "model",
                "event",
                "exception",
                "policy",
                "service"
        )) {
            assertTrue(
                    actual.contains(required),
                    () ->
                            "Missing verification domain "
                                    + "package: "
                                    + required
            );
        }
    }

    @Test
    void observationDomainUsesOnlyApprovedSubpackages()
            throws IOException {

        Set<String> actual =
                directDirectoriesContainingJavaSources(
                        OBSERVATION_DOMAIN
                );

        assertTrue(
                ALLOWED_OBSERVATION_DOMAIN_PACKAGES
                        .containsAll(actual),
                () ->
                        "Unexpected observation domain "
                                + "packages: "
                                + difference(
                                actual,
                                ALLOWED_OBSERVATION_DOMAIN_PACKAGES
                        )
        );

        for (String required : List.of(
                "model",
                "exception",
                "policy"
        )) {
            assertTrue(
                    actual.contains(required),
                    () ->
                            "Missing observation domain "
                                    + "package: "
                                    + required
            );
        }
    }

    @Test
    void managementDomainUsesOnlyApprovedSubpackages()
            throws IOException {

        Set<String> actual =
                directDirectoriesContainingJavaSources(
                        MANAGEMENT_DOMAIN
                );

        assertTrue(
                ALLOWED_MANAGEMENT_DOMAIN_PACKAGES
                        .containsAll(actual),
                () ->
                        "Unexpected management domain "
                                + "packages: "
                                + difference(
                                actual,
                                ALLOWED_MANAGEMENT_DOMAIN_PACKAGES
                        )
        );

        for (String required : List.of(
                "model",
                "exception",
                "repository"
        )) {
            assertTrue(
                    actual.contains(required),
                    () ->
                            "Missing management domain "
                                    + "package: "
                                    + required
            );
        }
    }

    @Test
    void capabilityDomainsRemainFrameworkAgnostic()
            throws IOException {

        for (Path domain : List.of(
                VERIFICATION_DOMAIN,
                OBSERVATION_DOMAIN,
                MANAGEMENT_DOMAIN
        )) {
            assertSourcesDoNotContain(
                    domain,
                    FORBIDDEN_DOMAIN_TOKENS
            );
        }
    }

    @Test
    void capabilityDomainsNeverObtainCurrentTime()
            throws IOException {

        for (Path domain : List.of(
                VERIFICATION_DOMAIN,
                OBSERVATION_DOMAIN,
                MANAGEMENT_DOMAIN
        )) {
            assertSourcesDoNotContain(
                    domain,
                    FORBIDDEN_CURRENT_TIME_TOKENS
            );
        }
    }

    @Test
    void verificationDomainDoesNotDependOnPaymentOrObservation()
            throws IOException {

        assertSourcesDoNotContain(
                VERIFICATION_DOMAIN,
                List.of(
                        "import com.sixpay.payment.",
                        "import com.sixpay.customer.observation."
                )
        );
    }

    @Test
    void observationDomainDoesNotDependOnPaymentOrVerification()
            throws IOException {

        assertSourcesDoNotContain(
                OBSERVATION_DOMAIN,
                List.of(
                        "import com.sixpay.payment.",
                        "import com.sixpay.customer.verification."
                )
        );
    }

    @Test
    void managementDomainDoesNotDependOnOtherCustomerCapabilities()
            throws IOException {

        /*
         * Management may orchestrate Verification from the
         * application layer, but the Management DOMAIN must
         * remain independent from Verification and Observation.
         */
        assertSourcesDoNotContain(
                MANAGEMENT_DOMAIN,
                List.of(
                        "import com.sixpay.customer.verification.",
                        "import com.sixpay.customer.observation."
                )
        );
    }

    @Test
    void verificationDomainContainsNoHttpClientOrJpaConcept()
            throws IOException {

        assertSourcesDoNotContain(
                VERIFICATION_DOMAIN,
                List.of(
                        "RestClient",
                        "WebClient",
                        "HttpClient",
                        "java.net.http",
                        "jakarta.persistence",
                        "EntityManager",
                        "JdbcTemplate",
                        "@Entity",
                        "@Repository"
                )
        );
    }

    @Test
    void observationDomainContainsNoHttpClientOrJpaConcept()
            throws IOException {

        assertSourcesDoNotContain(
                OBSERVATION_DOMAIN,
                List.of(
                        "RestClient",
                        "WebClient",
                        "HttpClient",
                        "java.net.http",
                        "jakarta.persistence",
                        "EntityManager",
                        "JdbcTemplate",
                        "@Entity",
                        "@Repository"
                )
        );
    }

    @Test
    void managementDomainContainsNoHttpClientOrJpaConcept()
            throws IOException {

        assertSourcesDoNotContain(
                MANAGEMENT_DOMAIN,
                List.of(
                        "RestClient",
                        "WebClient",
                        "HttpClient",
                        "java.net.http",
                        "jakarta.persistence",
                        "EntityManager",
                        "JdbcTemplate",
                        "@Entity",
                        "@Repository"
                )
        );
    }

    @Test
    void verificationEventPayloadRemainsSafe()
            throws IOException {

        Path event =
                VERIFICATION_DOMAIN.resolve(
                        "event/"
                                + "CustomerVerificationCompleted.java"
                );

        assertTrue(
                Files.isRegularFile(event)
        );

        String source =
                Files.readString(event);

        for (String forbidden : List.of(
                "CustomerNiu ",
                "CustomerIdentity ",
                "String legalName",
                "String accountNumber",
                "String phone",
                "String email",
                "String password",
                "String token",
                "String credential",
                "String rawResponse",
                "Amplitude"
        )) {
            assertFalse(
                    source.contains(forbidden),
                    () ->
                            "Sensitive event payload "
                                    + "concept found: "
                                    + forbidden
            );
        }

        for (String required : List.of(
                "CustomerVerificationId",
                "VerificationOutcome",
                "List<VerificationCheck>",
                "VerificationEvidenceFingerprint",
                "AccountBindingFingerprint",
                "Instant completedAt"
        )) {
            assertTrue(
                    source.contains(required),
                    () ->
                            "Missing safe event payload "
                                    + "concept: "
                                    + required
            );
        }
    }

    @Test
    void observationAccountModelContainsNoRawAccountConcept()
            throws IOException {

        Path accountReference =
                OBSERVATION_DOMAIN.resolve(
                        "model/"
                                + "ObservedAccountReference.java"
                );

        assertTrue(
                Files.isRegularFile(accountReference)
        );

        String source =
                Files.readString(accountReference);

        for (String forbidden : List.of(
                "accountNumber",
                "ribDebiteur",
                "rawAccount",
                "iban",
                "IntegrationAccountToken",
                "BankingAccountAccessReference"
        )) {
            assertFalse(
                    source.contains(forbidden),
                    () ->
                            "Raw account concept found: "
                                    + forbidden
            );
        }

        assertTrue(
                source.contains(
                        "accountBindingFingerprint"
                )
        );

        assertTrue(
                source.contains(
                        "maskedValue"
                )
        );
    }

    @Test
    void legacyApplicationsDoNotDependOnApiInfrastructureOrConfiguration()
            throws IOException {

        for (String capability : LEGACY_CAPABILITIES) {

            String prefix =
                    "import com.sixpay.customer."
                            + capability
                            + ".";

            assertSourcesDoNotContain(
                    JAVA_ROOT
                            .resolve(capability)
                            .resolve("application"),
                    List.of(
                            prefix + "api.",
                            prefix + "infrastructure.",
                            prefix + "configuration."
                    )
            );
        }
    }

    @Test
    void managementApplicationDoesNotDependOnOuterLayers()
            throws IOException {

        assertSourcesDoNotContain(
                MANAGEMENT_ROOT.resolve(
                        "application"
                ),
                List.of(
                        "import com.sixpay.customer.management.api.",
                        "import com.sixpay.customer.management.configuration.",
                        "import com.sixpay.customer.management.infrastructure."
                )
        );
    }

    @Test
    void legacyInfrastructureDoesNotDependOnApiOrConfiguration()
            throws IOException {

        for (String capability : LEGACY_CAPABILITIES) {

            String prefix =
                    "import com.sixpay.customer."
                            + capability
                            + ".";

            assertSourcesDoNotContain(
                    JAVA_ROOT
                            .resolve(capability)
                            .resolve("infrastructure"),
                    List.of(
                            prefix + "api.",
                            prefix + "configuration."
                    )
            );
        }
    }

    @Test
    void managementInfrastructureDoesNotDependOnApi()
            throws IOException {

        assertSourcesDoNotContain(
                MANAGEMENT_ROOT.resolve(
                        "infrastructure"
                ),
                List.of(
                        "import com.sixpay.customer.management.api."
                )
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
    void customerDoesNotDeclareAnotherBusinessDomainAsMavenDependency()
            throws IOException {

        String pom =
                Files.readString(
                        Path.of("pom.xml")
                );

        for (String forbiddenArtifactId : List.of(
                "payment",
                "partner",
                "subscription",
                "accounting",
                "reporting",
                "notification",
                "administration"
        )) {
            assertFalse(
                    pom.contains(
                            "<artifactId>"
                                    + forbiddenArtifactId
                                    + "</artifactId>"
                    ),
                    () ->
                            "Customer must not depend "
                                    + "on business module: "
                                    + forbiddenArtifactId
            );
        }
    }

    @Test
    void moduleDeclaresTheGoldenPlatformAndTestFoundation()
            throws IOException {

        String pom =
                Files.readString(
                        Path.of("pom.xml")
                );

        for (String artifactId : List.of(
                "common",
                "shared-kernel",
                "security",
                "integration",
                "spring-boot-starter-webmvc",
                "spring-boot-starter-restclient",
                "spring-boot-starter-oauth2-client",
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
                            "<artifactId>"
                                    + artifactId
                                    + "</artifactId>"
                    ),
                    () ->
                            "Missing Customer dependency: "
                                    + artifactId
            );
        }
    }

    @Test
    void moduleIsNotAnExecutableSpringBootApplication()
            throws IOException {

        assertSourcesDoNotContain(
                JAVA_ROOT,
                List.of(
                        "@SpringBootApplication"
                )
        );
    }

    private static Set<String>
    directDirectoriesContainingJavaSources(
            Path root
    ) throws IOException {

        if (!Files.isDirectory(root)) {
            return Set.of();
        }

        try (Stream<Path> paths =
                     Files.list(root)) {

            return paths
                    .filter(
                            Files::isDirectory
                    )
                    .filter(
                            CustomerArchitectureTest
                                    ::containsJavaSources
                    )
                    .map(
                            path ->
                                    path
                                            .getFileName()
                                            .toString()
                    )
                    .collect(
                            Collectors.toSet()
                    );
        }
    }

    private static boolean containsJavaSources(
            Path root
    ) {

        if (!Files.isDirectory(root)) {
            return false;
        }

        try (Stream<Path> paths =
                     Files.walk(root)) {

            return paths.anyMatch(
                    path ->
                            Files.isRegularFile(path)
                                    && path
                                    .toString()
                                    .endsWith(
                                            ".java"
                                    )
            );

        } catch (IOException exception) {

            throw new IllegalStateException(
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

        List<String> violations;

        try (Stream<Path> paths =
                     Files.walk(root)) {

            violations =
                    paths
                            .filter(
                                    Files::isRegularFile
                            )
                            .filter(
                                    path ->
                                            path
                                                    .toString()
                                                    .endsWith(
                                                            ".java"
                                                    )
                            )
                            .flatMap(
                                    path ->
                                            violations(
                                                    path,
                                                    forbiddenTokens
                                            )
                                                    .stream()
                            )
                            .toList();
        }

        assertTrue(
                violations.isEmpty(),
                () ->
                        "Architecture violations: "
                                + violations
        );
    }

    private static List<String> violations(
            Path path,
            List<String> forbiddenTokens
    ) {

        try {

            String source =
                    Files.readString(path);

            return forbiddenTokens
                    .stream()
                    .filter(
                            source::contains
                    )
                    .map(
                            token ->
                                    path
                                            + " contains forbidden token "
                                            + token
                    )
                    .toList();

        } catch (IOException exception) {

            throw new IllegalStateException(
                    "Cannot inspect "
                            + path,
                    exception
            );
        }
    }

    private static Set<String> difference(
            Set<String> left,
            Set<String> right
    ) {

        return left
                .stream()
                .filter(
                        value ->
                                !right.contains(value)
                )
                .collect(
                        Collectors.toSet()
                );
    }
}