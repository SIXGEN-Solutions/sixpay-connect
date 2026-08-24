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

    private static final Pattern SECURITY_PROPERTY_REFERENCE =
            Pattern.compile(
                    "sixpay\\.security\\.[A-Za-z0-9_.-]+"
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

        assertTrue(text.contains("DEFAULT_MAXIMUM_FAILED_ATTEMPTS = 5"));
        assertTrue(text.contains("Duration.ofMinutes(15)"));
        assertTrue(text.contains("DEFAULT_BCRYPT_STRENGTH = 12"));
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

        assertTrue(text.contains("DEFAULT_MIN_LENGTH = 12"));
        assertTrue(text.contains("DEFAULT_MAX_LENGTH = 200"));
        assertTrue(text.contains("DEFAULT_HISTORY_SIZE = 5"));
        assertTrue(text.contains("DEFAULT_EXPIRATION_DAYS = 90"));

        assertTrue(
                text.contains("new PasswordPolicy("),
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

        assertLocalAndOidc(local, true, false, "local-auth");
        assertLocalAndOidc(hybrid, true, true, "hybrid-auth");
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

        assertTrue(
                hybrid.contains("spring:")
                        && hybrid.contains("security:")
                        && hybrid.contains("oauth2:")
                        && hybrid.contains("resourceserver:"),
                "OAuth2 runtime assembly must remain in Bootstrap profile"
        );

        assertTrue(
                hybrid.contains("server:")
                        && hybrid.contains("servlet:")
                        && hybrid.contains("session:"),
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

                    Matcher matcher =
                            SECURITY_PROPERTY_REFERENCE.matcher(source);

                    while (matcher.find()) {
                        violations.add(
                                module
                                        + " directly consumes "
                                        + matcher.group()
                                        + " in "
                                        + javaFile
                        );
                    }
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

        assertTrue(localIndex >= 0, profile + " missing local section");
        assertTrue(oidcIndex > localIndex, profile + " missing OIDC section");

        String localSection =
                source.substring(
                        localIndex,
                        oidcIndex
                );

        String oidcSection =
                source.substring(oidcIndex);

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
