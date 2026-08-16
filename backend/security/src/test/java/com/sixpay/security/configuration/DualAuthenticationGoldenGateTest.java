package com.sixpay.security.configuration;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DA-11.6 — Golden closure gate for Dual Authentication — Local + OIDC.
 *
 * <p>This gate does not duplicate the behavioral assertions of DA-11.1 to
 * DA-11.5. It verifies that the canonical integration/security evidence and
 * closure documentation remain present in the security module.</p>
 *
 * <p>The behavioral tests themselves are executed by Maven Failsafe through
 * the repository's existing {@code full-tests} profile.</p>
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

    private static final Path CLOSURE_DOCUMENT =
            Path.of(
                    "DA-11-INTEGRATION-SECURITY-CLOSURE.md"
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

        assertThat(
                Files.exists(
                        CLOSURE_DOCUMENT
                )
        )
                .as(
                        "DA-11 closure documentation must remain in backend/security"
                )
                .isTrue();

        String documentation =
                Files.readString(
                        CLOSURE_DOCUMENT
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
