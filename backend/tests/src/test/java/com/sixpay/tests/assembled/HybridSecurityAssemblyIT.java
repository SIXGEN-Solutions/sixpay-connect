package com.sixpay.tests.assembled;

import com.sixpay.security.application.port.in.AuthenticateLocalUserUseCase;
import com.sixpay.security.application.port.in.GetCurrentSessionUseCase;
import com.sixpay.security.application.port.out.ExternalIdentityResolver;
import com.sixpay.security.configuration.AuthenticationCapabilitiesProperties;
import com.sixpay.security.infrastructure.authentication.oidc.OidcAuthenticationAdapter;
import com.sixpay.security.infrastructure.authentication.session.RestrictedLocalSessionFilter;
import com.sixpay.tests.support.CrossModulePostgreSqlTestSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 8.3.5 — Hybrid Security Assembly.
 *
 * <p>Validates that the fully assembled backend can enable Local and OIDC
 * authentication simultaneously without changing the neutral assembled-test
 * baseline used by 8.3.1–8.3.4.</p>
 *
 * <p>The OIDC decoder is the only test-owned infrastructure dependency. It
 * prevents any network call to an external identity provider while preserving
 * the real SIXPAY OIDC adapter, identity resolver, authorization mapping,
 * session infrastructure and SecurityFilterChain.</p>
 */
@SpringBootTest(
        classes = AssembledApplicationContextIT.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "sixpay.security.authentication.local.enabled=true",
                "sixpay.security.authentication.oidc.enabled=true",
                "sixpay.security.authentication.oidc.registration-id=sixpay"
        }
)
@ActiveProfiles("assembled-test")
@EnabledIfSystemProperty(
        named = "sixpay.assembled.tests",
        matches = "true"
)
@Import(HybridSecurityAssemblyIT.HybridSecurityTestConfiguration.class)
class HybridSecurityAssemblyIT
        extends CrossModulePostgreSqlTestSupport {

    @Autowired
    private ApplicationContext context;

    @Autowired
    private AuthenticationCapabilitiesProperties capabilities;

    @Autowired
    private SecurityFilterChain securityFilterChain;

    @Test
    void assembledApplicationEnablesHybridAuthenticationCapabilities() {
        assertThat(capabilities.localEnabled()).isTrue();
        assertThat(capabilities.oidcEnabled()).isTrue();
        assertThat(capabilities.hybridEnabled()).isTrue();

        assertThat(context.getBeansOfType(
                AuthenticateLocalUserUseCase.class
        )).hasSize(1);

        assertThat(context.getBeansOfType(
                OidcAuthenticationAdapter.class
        )).hasSize(1);

        assertThat(context.getBeansOfType(
                ExternalIdentityResolver.class
        )).hasSize(1);

        assertThat(context.getBeansOfType(
                GetCurrentSessionUseCase.class
        )).hasSize(1);
    }

    @Test
    void assembledApplicationKeepsOneSecurityChainForLocalAndBearerAuthentication() {
        assertThat(context.getBeansOfType(
                SecurityFilterChain.class
        )).hasSize(1);

        assertThat(securityFilterChain.getFilters())
                .anyMatch(RestrictedLocalSessionFilter.class::isInstance)
                .anyMatch(BearerTokenAuthenticationFilter.class::isInstance);
    }

    @Test
    void localLoginBoundaryRemainsExposedInsideHybridAssembly() {
        RequestMappingHandlerMapping mappings =
                context.getBean(
                        "requestMappingHandlerMapping",
                        RequestMappingHandlerMapping.class
                );

        boolean localLoginPresent =
                mappings.getHandlerMethods()
                        .keySet()
                        .stream()
                        .anyMatch(mapping ->
                                mapping
                                        .getMethodsCondition()
                                        .getMethods()
                                        .contains(RequestMethod.POST)
                                        && mapping
                                        .getPatternValues()
                                        .contains("/api/v1/auth/login")
                        );

        assertThat(localLoginPresent).isTrue();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class HybridSecurityTestConfiguration {

        /**
         * OIDC must be assembled, but 8.3.5 must not depend on a live IdP.
         * The real OidcAuthenticationAdapter still participates in the chain;
         * only token decoding is replaced at the external boundary.
         */
        @Bean
        JwtDecoder assembledHybridJwtDecoder() {
            return token -> {
                throw new JwtException(
                        "Token decoding is not exercised by the assembly gate"
                );
            };
        }
    }
}
