package com.sixpay.security.configuration;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DA-11.6 / DA-12 — Golden closure and documentation validation gate for
 * Dual Authentication — Local + OIDC.
 *
 * <p>The gate does not duplicate behavioral assertions. Focused unit and
 * integration tests remain the source of behavioral evidence; this class
 * verifies that the canonical evidence and final closure documentation remain
 * present and synchronized.</p>
 */
class DualAuthenticationGoldenGateTest {

    private static final List<String> REQUIRED_INTEGRATION_TESTS =
            List.of(
                    "com.sixpay.security.integration.AuthenticationCapabilityMatrixIT",
                    "com.sixpay.security.integration.LocalAuthenticationSessionIT",
                    "com.sixpay.security.configuration.OidcAuthenticationProviderIT",
                    "com.sixpay.security.configuration.HybridAuthenticationIT",
                    "com.sixpay.security.configuration.SecurityAuthorizationBoundaryIT"
            );

    private static final List<String> REQUIRED_REGRESSION_TESTS =
            List.of(
                    "com.sixpay.security.configuration.SixpaySecurityAutoConfigurationTest",
                    "com.sixpay.security.configuration.AuditingAuthenticationEntryPointTest"
            );

    private static final Path DA11_CLOSURE_DOCUMENT =
            Path.of(
                    "DA-11-INTEGRATION-SECURITY-CLOSURE.md"
            );

    private static final Path DA12_CLOSURE_DOCUMENT =
            Path.of(
                    "DA-12-DUAL-AUTHENTICATION-CLOSURE.md"
            );

    private static final Path SECURITY_COVERAGE_DOCUMENT =
            Path.of(
                    "SECURITY-TEST-COVERAGE.md"
            );

    @Test
    void requiresAllDa11IntegrationSecurityEvidence() {
        assertLoadableTestClasses(
                REQUIRED_INTEGRATION_TESTS
        );
    }

    @Test
    void requiresCriticalSecurityRegressionEvidence() {
        assertLoadableTestClasses(
                REQUIRED_REGRESSION_TESTS
        );
    }

    @Test
    void requiresCanonicalDa11ClosureDocument()
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
                        "DA-11 DUAL AUTHENTICATION INTEGRATION/SECURITY = COVERED"
                );
    }

    @Test
    void requiresCanonicalDa12FinalClosureDocument()
            throws IOException {

        String documentation =
                readRequiredDocument(
                        DA12_CLOSURE_DOCUMENT,
                        "DA-12 final Dual Authentication closure documentation"
                );

        assertThat(documentation)
                .contains(
                        "DA-12 — Documentation + validation gate",
                        "LOCAL + OIDC",
                        "SIXPAY owns authorization",
                        "LOCAL password lifecycle",
                        "OIDC password lifecycle is owned by the IdP",
                        "DualAuthenticationGoldenGateTest",
                        "DA-12 DOCUMENTATION + VALIDATION GATE = COVERED",
                        "DUAL AUTHENTICATION — LOCAL + OIDC = CLOSED"
                );
    }

    @Test
    void requiresSecurityCoverageToReferenceFinalDualAuthenticationClosure()
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
                        "DUAL AUTHENTICATION = COVERED"
                );
    }

    private static String readRequiredDocument(
            Path path,
            String description
    ) throws IOException {

        assertThat(
                Files.exists(path)
        )
                .as(
                        description
                                + " must remain input backend/security"
                )
                .isTrue();

        return Files.readString(path);
    }

    private static void assertLoadableTestClasses(
            List<String> classNames
    ) {
        for (String className : classNames) {
            assertThatCodeLoads(
                    className
            );
        }
    }

    private static void assertThatCodeLoads(
            String className
    ) {
        try {
            Class<?> testClass =
                    Class.forName(
                            className
                    );

            assertThat(
                    testClass
                            .getDeclaredMethods()
            )
                    .anyMatch(method ->
                            method.isAnnotationPresent(
                                    Test.class
                            )
                    );
        } catch (ClassNotFoundException exception) {
            throw new AssertionError(
                    "Required golden security evidence is missing: "
                            + className,
                    exception
            );
        }
    }
}
