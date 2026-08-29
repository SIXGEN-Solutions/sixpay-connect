package com.sixpay.security.integration;

import com.sixpay.security.application.port.input.AuthenticateLocalUserUseCase;
import com.sixpay.security.application.port.output.ExternalIdentityResolver;
import com.sixpay.security.application.port.output.PasswordHistoryPort;
import com.sixpay.security.application.port.output.SecurityAuditPort;
import com.sixpay.security.configuration.AuthenticationCapabilitiesProperties;
import com.sixpay.security.configuration.SixpaySecurityAutoConfiguration;
import com.sixpay.security.domain.authentication.PasswordPolicy;
import com.sixpay.security.infrastructure.authentication.audit.AuthenticationAuditSpringDataRepository;
import com.sixpay.security.infrastructure.authentication.oidc.OidcAuthenticationAdapter;
import com.sixpay.security.infrastructure.authentication.persistence.LocalAuthenticationUserSpringDataRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.data.jpa.autoconfigure.DataJpaRepositoriesAutoConfiguration;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class AuthenticationCapabilityMatrixIT {

    private final WebApplicationContextRunner contextRunner =
            new WebApplicationContextRunner()
                    .withUserConfiguration(
                            CapabilityMatrixTestApplication.class
                    );

    @Test
    void disablesBothAuthenticationCapabilities() {
        runMatrixCase(
                false,
                false,
                false,
                false
        );
    }

    @Test
    void enablesLocalAuthenticationOnly() {
        runMatrixCase(
                true,
                false,
                true,
                false
        );
    }

    @Test
    void enablesOidcAuthenticationOnly() {
        runMatrixCase(
                false,
                true,
                false,
                true
        );
    }

    @Test
    void enablesHybridLocalAndOidcAuthentication() {
        runMatrixCase(
                true,
                true,
                true,
                true
        );
    }

    private void runMatrixCase(
            boolean localEnabled,
            boolean oidcEnabled,
            boolean expectLocalBoundary,
            boolean expectOidcBoundary
    ) {
        contextRunner
                .withPropertyValues(
                        "sixpay.security.authentication.local.enabled="
                                + localEnabled,
                        "sixpay.security.authentication.oidc.enabled="
                                + oidcEnabled,
                        "sixpay.security.authentication.oidc.registration-id=sixpay"
                )
                .run(context -> {
                    assertThat(context)
                            .hasNotFailed();

                    AuthenticationCapabilitiesProperties capabilities =
                            context.getBean(
                                    AuthenticationCapabilitiesProperties.class
                            );

                    assertThat(
                            capabilities.localEnabled()
                    )
                            .isEqualTo(
                                    localEnabled
                            );

                    assertThat(
                            capabilities.oidcEnabled()
                    )
                            .isEqualTo(
                                    oidcEnabled
                            );

                    assertThat(
                            capabilities.hybridEnabled()
                    )
                            .isEqualTo(
                                    localEnabled
                                            && oidcEnabled
                            );

                    if (expectLocalBoundary) {
                        assertThat(context)
                                .hasSingleBean(
                                        AuthenticateLocalUserUseCase.class
                                );
                    } else {
                        assertThat(context)
                                .doesNotHaveBean(
                                        AuthenticateLocalUserUseCase.class
                                );
                    }

                    if (expectOidcBoundary) {
                        assertThat(context)
                                .hasSingleBean(
                                        OidcAuthenticationAdapter.class
                                );
                    } else {
                        assertThat(context)
                                .doesNotHaveBean(
                                        OidcAuthenticationAdapter.class
                                );
                    }

                    assertThat(
                            hasLocalLoginMapping(
                                    context
                            )
                    )
                            .isEqualTo(
                                    expectLocalBoundary
                            );
                });
    }

    private static boolean hasLocalLoginMapping(
            org.springframework.context.ApplicationContext context
    ) {
        RequestMappingHandlerMapping mappings =
                context.getBean(
                        RequestMappingHandlerMapping.class
                );

        return mappings
                .getHandlerMethods()
                .keySet()
                .stream()
                .anyMatch(mapping ->
                        mapping
                                .getMethodsCondition()
                                .getMethods()
                                .contains(
                                        RequestMethod.POST
                                )
                                && mapping
                                .getPatternValues()
                                .contains(
                                        "/api/v1/auth/login"
                                )
                );
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration(
            exclude = {
                    DataSourceAutoConfiguration.class,
                    HibernateJpaAutoConfiguration.class,
                    DataJpaRepositoriesAutoConfiguration.class
            }
    )
    @Import(
            SixpaySecurityAutoConfiguration.class
    )
    static class CapabilityMatrixTestApplication {

        @Bean
        SecurityAuditPort securityAuditPort() {
            return mock(
                    SecurityAuditPort.class
            );
        }

        @Bean
        ExternalIdentityResolver externalIdentityResolver() {
            return mock(
                    ExternalIdentityResolver.class
            );
        }

        @Bean
        JwtDecoder jwtDecoder() {
            return mock(
                    JwtDecoder.class
            );
        }

        @Bean
        LocalAuthenticationUserSpringDataRepository
        localAuthenticationUserSpringDataRepository() {
            return mock(
                    LocalAuthenticationUserSpringDataRepository.class
            );
        }

        @Bean
        AuthenticationAuditSpringDataRepository
        authenticationAuditSpringDataRepository() {
            return mock(
                    AuthenticationAuditSpringDataRepository.class
            );
        }

        @Bean
        PasswordHistoryPort passwordHistoryPort() {
            return mock(
                    PasswordHistoryPort.class
            );
        }

        @Bean
        PasswordPolicy passwordPolicy() {
            return new PasswordPolicy(
                    12,
                    200,
                    5,
                    90
            );
        }
    }
}
