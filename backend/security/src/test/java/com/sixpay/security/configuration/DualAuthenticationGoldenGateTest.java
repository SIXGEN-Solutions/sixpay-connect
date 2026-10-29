package com.sixpay.security.configuration;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AUTH-13 — Golden documentation/evidence gate for the final
 * Hybrid Authentication baseline — LOCAL + OIDC + LDAP.
 *
 * <p>Historical DA-11/DA-12 Local/OIDC closure remains valid evidence.
 * AUTH-12 extends that baseline with LDAP; AUTH-13 verifies that both the
 * historical evidence and the LDAP extension remain present.</p>
 */
class DualAuthenticationGoldenGateTest {

    private static final List<String> REQUIRED_INTEGRATION_TESTS =
            List.of(
                    "com.sixpay.security.integration.AuthenticationCapabilityMatrixIT",
                    "com.sixpay.security.integration.LocalAuthenticationSessionIT",
                    "com.sixpay.security.configuration.OidcAuthenticationProviderIT",
                    "com.sixpay.security.configuration.HybridAuthenticationIT",
                    "com.sixpay.security.api.controller.LdapAuthenticationFailureIT",
                    "com.sixpay.security.application.service.LdapCanonicalAuthenticationServiceTest",
                    "com.sixpay.security.infrastructure.authentication.ldap.ActiveDirectoryLdapAuthenticationAdapterTest",
                    "com.sixpay.security.configuration.SecurityAuthorizationBoundaryIT"
            );

    private static final List<String> REQUIRED_REGRESSION_TESTS =
            List.of(
                    "com.sixpay.security.configuration.SixpaySecurityAutoConfigurationTest",
                    "com.sixpay.security.configuration.AuditingAuthenticationEntryPointTest"
            );

    private static final Path DA11_CLOSURE_DOCUMENT =
            Path.of("DA-11-INTEGRATION-SECURITY-CLOSURE.md");

    private static final Path DA12_CLOSURE_DOCUMENT =
            Path.of("DA-12-DUAL-AUTHENTICATION-CLOSURE.md");

    private static final Path SECURITY_COVERAGE_DOCUMENT =
            Path.of("SECURITY-TEST-COVERAGE.md");

    @Test
    void requiresHybridAuthenticationExecutableEvidence() {
        assertLoadableTestClasses(REQUIRED_INTEGRATION_TESTS);
        assertLoadableTestClasses(REQUIRED_REGRESSION_TESTS);
    }

    @Test
    void requiresHistoricalDa11EvidenceAndAuth12LdapExtension()
            throws IOException {

        String documentation =
                readRequiredDocument(
                        DA11_CLOSURE_DOCUMENT,
                        "DA-11 closure documentation"
                );

        assertThat(documentation)
                .contains(
                        "DA-11.1 — Capability matrix",
                        "DA-11.2 — Local session integration",
                        "DA-11.3 — OIDC integration",
                        "DA-11.4 — Hybrid coexistence",
                        "DA-11.5 — Authorization + CSRF",
                        "AUTH-12 LDAP EXTENSION",
                        "LdapAuthenticationFailureIT",
                        "LdapCanonicalAuthenticationServiceTest",
                        "ActiveDirectoryLdapAuthenticationAdapterTest"
                );
    }

    @Test
    void requiresDa12HistoricalClosureAndAuth13FinalBaseline()
            throws IOException {

        String documentation =
                readRequiredDocument(
                        DA12_CLOSURE_DOCUMENT,
                        "DA-12 closure documentation"
                );

        assertThat(documentation)
                .contains(
                        "DA-12 — Documentation + validation gate",
                        "SIXPAY owns authorization",
                        "LOCAL password lifecycle",
                        "DualAuthenticationGoldenGateTest",
                        "AUTH-13 FINAL HYBRID BASELINE",
                        "LOCAL + OIDC + LDAP",
                        "OIDC and LDAP password lifecycle are provider-owned",
                        "HYBRID AUTHENTICATION — LOCAL + OIDC + LDAP = CLOSED"
                );
    }

    @Test
    void requiresSecurityCoverageToReferenceFinalHybridAuthenticationClosure()
            throws IOException {

        String documentation =
                readRequiredDocument(
                        SECURITY_COVERAGE_DOCUMENT,
                        "security golden test coverage documentation"
                );

        assertThat(documentation)
                .contains(
                        "DA-11 Integration/security evidence",
                        "DA-12 Documentation + validation gate",
                        "DA-12-DUAL-AUTHENTICATION-CLOSURE.md",
                        "DualAuthenticationGoldenGateTest",
                        "HYBRID AUTHENTICATION LOCAL/OIDC/LDAP = COVERED"
                );
    }

    private static String readRequiredDocument(Path path, String description)
            throws IOException {

        assertThat(Files.exists(path))
                .as(description + " must remain in backend/security")
                .isTrue();

        return Files.readString(path);
    }

    private static void assertLoadableTestClasses(List<String> classNames) {
        for (String className : classNames) {
            assertThatCodeLoads(className);
        }
    }

    private static void assertThatCodeLoads(String className) {
        try {
            Class<?> testClass = Class.forName(className);

            assertThat(testClass.getDeclaredMethods())
                    .anyMatch(method -> method.isAnnotationPresent(Test.class));
        } catch (ClassNotFoundException exception) {
            throw new AssertionError(
                    "Required golden security evidence is missing: " + className,
                    exception
            );
        }
    }
}
