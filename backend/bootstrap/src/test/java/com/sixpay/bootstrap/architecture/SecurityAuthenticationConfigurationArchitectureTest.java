package com.sixpay.bootstrap.architecture;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityAuthenticationConfigurationArchitectureTest {

    private static final Path BACKEND_ROOT =
            Path.of("..");

    private static final Path BOOTSTRAP_RESOURCES =
            Path.of("src/main/resources");

    private static final List<String> BUSINESS_MODULES =
            List.of(
                    "partner",
                    "customer",
                    "payment",
                    "accounting",
                    "reporting",
                    "notification",
                    "administration"
            );

    private static final Pattern VALUE_PROPERTY =
            Pattern.compile(
                    "@Value\\s*\\(\\s*\"\\$\\{"
                            + "(sixpay\\.security\\.[^}:]+)"
            );

    private static final Pattern CONFIGURATION_PROPERTIES =
            Pattern.compile(
                    "@ConfigurationProperties\\s*\\("
                            + "\\s*(?:prefix\\s*=\\s*)?"
                            + "\"(sixpay\\.security\\.[^\"]+)\"",
                    Pattern.DOTALL
            );

    private static final Pattern CONDITIONAL_ON_PROPERTY =
            Pattern.compile(
                    "@ConditionalOnProperty\\s*\\((.*?)\\)",
                    Pattern.DOTALL
            );

    @Test
    void securityOwnsAuthenticationCapabilityProperties()
            throws Exception {

        Path source =
                BACKEND_ROOT.resolve(
                        "security/src/main/java/com/sixpay/security/"
                                + "configuration/"
                                + "AuthenticationCapabilitiesProperties.java"
                );

        assertTrue(Files.isRegularFile(source));

        String text = Files.readString(source);

        assertTrue(
                text.contains(
                        "@ConfigurationProperties("
                                + "\"sixpay.security.authentication\""
                                + ")"
                ),
                "Security must own authentication capability binding"
        );

        assertTrue(
                text.contains(
                        "DEFAULT_MAXIMUM_FAILED_ATTEMPTS = 5"
                )
        );

        assertTrue(
                text.contains(
                        "Duration.ofMinutes(15)"
                )
        );

        assertTrue(
                text.contains(
                        "DEFAULT_BCRYPT_STRENGTH = 12"
                )
        );
    }

    @Test
    void securityOwnsPasswordPolicyProperties()
            throws Exception {

        Path source =
                BACKEND_ROOT.resolve(
                        "security/src/main/java/com/sixpay/security/"
                                + "configuration/"
                                + "PasswordPolicyProperties.java"
                );

        assertTrue(Files.isRegularFile(source));

        String text = Files.readString(source);

        assertTrue(
                text.contains(
                        "@ConfigurationProperties("
                                + "\"sixpay.security.local.password\""
                                + ")"
                ),
                "Security must own password-policy binding"
        );

        assertTrue(
                text.contains(
                        "DEFAULT_MIN_LENGTH = 12"
                )
        );

        assertTrue(
                text.contains(
                        "DEFAULT_MAX_LENGTH = 200"
                )
        );

        assertTrue(
                text.contains(
                        "DEFAULT_HISTORY_SIZE = 5"
                )
        );

        assertTrue(
                text.contains(
                        "DEFAULT_EXPIRATION_DAYS = 90"
                )
        );

        assertTrue(
                text.contains(
                        "new PasswordPolicy("
                ),
                "External password configuration must be validated "
                        + "through Security domain invariants"
        );
    }

    @Test
    void authenticationProfilesPreserveReviewedSemantics()
            throws Exception {

        String local =
                Files.readString(
                        BOOTSTRAP_RESOURCES.resolve(
                                "application-local-auth.yml"
                        )
                );

        String hybrid =
                Files.readString(
                        BOOTSTRAP_RESOURCES.resolve(
                                "application-hybrid-auth.yml"
                        )
                );

        assertLocalAndOidc(
                local,
                true,
                false,
                "local-auth"
        );

        assertLocalAndOidc(
                hybrid,
                true,
                true,
                "hybrid-auth"
        );
    }

    @Test
    void hybridProfileKeepsRuntimeOauthAndSessionAssemblyInBootstrap()
            throws Exception {

        String hybrid =
                Files.readString(
                        BOOTSTRAP_RESOURCES.resolve(
                                "application-hybrid-auth.yml"
                        )
                );

        String localRuntime =
                Files.readString(
                        BOOTSTRAP_RESOURCES.resolve(
                                "config/security/local-auth-common.yml"
                        )
                );

        String oidcRuntime =
                Files.readString(
                        BOOTSTRAP_RESOURCES.resolve(
                                "config/security/oidc-common.yml"
                        )
                );

        assertTrue(
                hybrid.contains(
                        "classpath:config/security/oidc-common.yml"
                )
                        && oidcRuntime.contains("spring:")
                        && oidcRuntime.contains("security:")
                        && oidcRuntime.contains("oauth2:")
                        && oidcRuntime.contains("resourceserver:"),
                "OAuth2 runtime assembly must remain in Bootstrap profile"
        );

        assertTrue(
                hybrid.contains(
                        "classpath:config/security/local-auth-common.yml"
                ),
                "Hybrid profile must import the canonical local runtime"
        );

        assertTrue(
                localRuntime.contains("server:")
                        && localRuntime.contains("servlet:")
                        && localRuntime.contains("session:"),
                "HTTP session runtime assembly must remain in Bootstrap"
        );
    }

    @Test
    void otherBusinessModulesDoNotConsumeSecurityConfigurationDirectly()
            throws Exception {

        List<String> violations =
                new ArrayList<>();

        for (String module : BUSINESS_MODULES) {

            Path javaRoot =
                    BACKEND_ROOT.resolve(
                            module + "/src/main/java"
                    );

            if (!Files.isDirectory(javaRoot)) {
                continue;
            }

            try (var files = Files.walk(javaRoot)) {

                for (Path javaFile :
                        files.filter(Files::isRegularFile)
                                .filter(path ->
                                        path.toString()
                                                .endsWith(".java")
                                )
                                .toList()) {

                    String source =
                            Files.readString(javaFile);

                    findValueViolations(
                            module,
                            javaFile,
                            source,
                            violations
                    );

                    findConfigurationPropertiesViolations(
                            module,
                            javaFile,
                            source,
                            violations
                    );

                    findConditionalPropertyViolations(
                            module,
                            javaFile,
                            source,
                            violations
                    );
                }
            }
        }

        assertTrue(
                violations.isEmpty(),
                () -> "Security configuration leaked into other "
                        + "business modules: "
                        + violations
        );
    }

    private void findValueViolations(
            String module,
            Path javaFile,
            String source,
            List<String> violations
    ) {

        Matcher matcher =
                VALUE_PROPERTY.matcher(source);

        while (matcher.find()) {

            violations.add(
                    module
                            + " consumes Security configuration "
                            + matcher.group(1)
                            + " through @Value in "
                            + javaFile
            );
        }
    }

    private void findConfigurationPropertiesViolations(
            String module,
            Path javaFile,
            String source,
            List<String> violations
    ) {

        Matcher matcher =
                CONFIGURATION_PROPERTIES.matcher(
                        source
                );

        while (matcher.find()) {

            violations.add(
                    module
                            + " consumes Security configuration "
                            + matcher.group(1)
                            + " through @ConfigurationProperties in "
                            + javaFile
            );
        }
    }

    private void findConditionalPropertyViolations(
            String module,
            Path javaFile,
            String source,
            List<String> violations
    ) {

        Matcher annotation =
                CONDITIONAL_ON_PROPERTY.matcher(
                        source
                );

        while (annotation.find()) {

            String body =
                    annotation.group(1);

            Matcher prefixMatcher =
                    Pattern.compile(
                            "prefix\\s*=\\s*"
                                    + "\"(sixpay\\.security\\.[^\"]+)\""
                    ).matcher(body);

            if (prefixMatcher.find()) {

                violations.add(
                        module
                                + " consumes Security configuration "
                                + prefixMatcher.group(1)
                                + " through @ConditionalOnProperty in "
                                + javaFile
                );
            }
        }
    }

    private void assertLocalAndOidc(
            String source,
            boolean localExpected,
            boolean oidcExpected,
            String profile
    ) {

        int localIndex =
                source.indexOf("local:");

        int oidcIndex =
                source.indexOf("oidc:");

        assertTrue(
                localIndex >= 0,
                profile + " missing local section"
        );

        assertTrue(
                oidcIndex > localIndex,
                profile + " missing OIDC section"
        );

        String localSection =
                source.substring(
                        localIndex,
                        oidcIndex
                );

        String oidcSection =
                source.substring(
                        oidcIndex
                );

        assertTrue(
                localSection.contains(
                        "enabled: " + localExpected
                ),
                profile + " local semantics changed"
        );

        assertTrue(
                oidcSection.contains(
                        "enabled: " + oidcExpected
                ),
                profile + " OIDC semantics changed"
        );
    }
}
