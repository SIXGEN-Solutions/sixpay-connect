package com.sixpay.tests.assembled;

import com.sixpay.security.application.port.in.AuthenticateLocalUserUseCase;
import com.sixpay.security.configuration.AuthenticationCapabilitiesProperties;
import com.sixpay.security.infrastructure.authentication.oidc.OidcAuthenticationAdapter;
import com.sixpay.security.infrastructure.authentication.session.RestrictedLocalSessionFilter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@SpringBootTest(
        classes = HybridSecurityAssemblyIT.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles({
        "assembled-test",
        "hybrid-security-assembled"
})
@Testcontainers
@EnabledIfSystemProperty(
        named = "sixpay.assembled.tests",
        matches = "true"
)
@Import(HybridSecurityAssemblyIT.HybridSecurityTestConfiguration.class)
class HybridSecurityAssemblyIT {

    @Container
    static final PostgreSQLContainer POSTGRES =
            new PostgreSQLContainer(
                    DockerImageName.parse("postgres:15-alpine")
            )
                    .withDatabaseName("sixpay_hybrid_security")
                    .withUsername("sixpay")
                    .withPassword("sixpay-test");

    @DynamicPropertySource
    static void databaseProperties(
            DynamicPropertyRegistry registry
    ) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private ApplicationContext context;

    @Autowired
    private AuthenticationCapabilitiesProperties capabilities;

    @Autowired
    private RequestMappingHandlerMapping mappings;

    @Autowired
    private SecurityFilterChain securityFilterChain;

    @Test
    void assembledApplicationActivatesHybridAuthenticationCapabilities() {
        assertThat(capabilities.localEnabled()).isTrue();
        assertThat(capabilities.oidcEnabled()).isTrue();
        assertThat(capabilities.hybridEnabled()).isTrue();

        assertThat(context.getBeansOfType(AuthenticateLocalUserUseCase.class))
                .as("local authentication boundary must be assembled")
                .hasSize(1);

        assertThat(context.getBeansOfType(OidcAuthenticationAdapter.class))
                .as("OIDC authentication boundary must be assembled")
                .hasSize(1);
    }

    @Test
    void hybridAssemblyKeepsSingleSecurityFilterChainWithBothMechanisms() {
        Map<String, SecurityFilterChain> chains =
                context.getBeansOfType(SecurityFilterChain.class);

        assertThat(chains)
                .as("hybrid assembly must not create competing filter chains")
                .hasSize(1);

        assertThat(securityFilterChain.getFilters())
                .anyMatch(filter -> filter instanceof RestrictedLocalSessionFilter)
                .anyMatch(filter -> filter instanceof BearerTokenAuthenticationFilter);
    }

    @Test
    void localAndOidcSessionBoundariesCoexistInTheAssembledApplication() {
        assertThat(hasMapping(RequestMethod.POST, "/api/v1/auth/login"))
                .as("local login boundary")
                .isTrue();

        assertThat(hasMapping(RequestMethod.POST, "/api/v1/auth/session/oidc"))
                .as("OIDC session establishment boundary")
                .isTrue();

        assertThat(hasMapping(RequestMethod.GET, "/api/v1/auth/me"))
                .as("mechanism-neutral session bootstrap boundary")
                .isTrue();

        assertThat(hasMapping(RequestMethod.POST, "/api/v1/auth/logout"))
                .as("mechanism-neutral logout boundary")
                .isTrue();
    }

    private boolean hasMapping(
            RequestMethod method,
            String path
    ) {
        return mappings
                .getHandlerMethods()
                .keySet()
                .stream()
                .anyMatch(mapping ->
                        mapping.getMethodsCondition()
                                .getMethods()
                                .contains(method)
                                && mapping.getPatternValues()
                                .contains(path)
                );
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class HybridSecurityTestConfiguration {

        @Bean
        @Primary
        JwtDecoder hybridSecurityJwtDecoder() {
            return mock(JwtDecoder.class);
        }
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @ComponentScan(basePackages = {
            "com.sixpay.common",
            "com.sixpay.partner",
            "com.sixpay.customer",
            "com.sixpay.payment",
            "com.sixpay.accounting",
            "com.sixpay.reporting",
            "com.sixpay.notification",
            "com.sixpay.administration",
            "com.sixpay.security",
            "com.sixpay.integration"
    })
    static class TestApplication {
    }
}
