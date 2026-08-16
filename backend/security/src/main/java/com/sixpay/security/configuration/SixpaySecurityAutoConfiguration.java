package com.sixpay.security.configuration;

import com.sixpay.security.api.controller.AuthenticationSessionController;
import com.sixpay.security.application.port.in.GetCurrentSessionUseCase;
import com.sixpay.security.application.port.out.ExternalIdentityResolver;
import com.sixpay.security.application.port.out.SecurityAuditPort;
import com.sixpay.security.application.service.CurrentSessionService;
import com.sixpay.security.authentication.CurrentUserProvider;
import com.sixpay.security.authentication.SecurityContextCurrentUserProvider;
import com.sixpay.security.infrastructure.authentication.oidc.OidcAuthenticationAdapter;
import com.sixpay.security.infrastructure.authentication.session.RestrictedLocalSessionFilter;
import com.sixpay.security.infrastructure.authentication.session.SpringSecuritySessionManager;
import com.sixpay.security.jwt.SixpayJwtAuthoritiesConverter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.ExceptionTranslationFilter;
import org.springframework.security.web.context.DelegatingSecurityContextRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.util.matcher.RequestMatcher;

@AutoConfiguration
@EnableMethodSecurity
@EnableConfigurationProperties(
        AuthenticationCapabilitiesProperties.class
)
@Import({
        LocalAuthenticationConfiguration.class,
        IdentityLinkingConfiguration.class,
        AuthenticationSessionController.class
})
@ConditionalOnClass({
        HttpSecurity.class,
        Jwt.class
})
@ConditionalOnWebApplication(
        type =
                ConditionalOnWebApplication.Type.SERVLET
)
public class SixpaySecurityAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    SixpayJwtAuthoritiesConverter
    sixpayJwtAuthoritiesConverter() {
        return new SixpayJwtAuthoritiesConverter();
    }

    @Bean
    @ConditionalOnMissingBean(
            JwtAuthenticationConverter.class
    )
    JwtAuthenticationConverter
    jwtAuthenticationConverter(
            SixpayJwtAuthoritiesConverter
                    authoritiesConverter
    ) {
        JwtAuthenticationConverter converter =
                new JwtAuthenticationConverter();

        converter
                .setJwtGrantedAuthoritiesConverter(
                        authoritiesConverter
                );

        return converter;
    }

    @Bean
    @ConditionalOnMissingBean(
            OidcAuthenticationAdapter.class
    )
    @ConditionalOnProperty(
            prefix =
                    "sixpay.security.authentication.oidc",
            name = "enabled",
            havingValue = "true"
    )
    OidcAuthenticationAdapter
    oidcAuthenticationAdapter(
            ExternalIdentityResolver
                    externalIdentityResolver,
            SecurityAuditPort auditPort
    ) {
        return new OidcAuthenticationAdapter(
                externalIdentityResolver,
                auditPort
        );
    }

    @Bean
    @ConditionalOnMissingBean(
            CurrentUserProvider.class
    )
    CurrentUserProvider
    currentUserProvider() {
        return new SecurityContextCurrentUserProvider();
    }

    @Bean
    @ConditionalOnMissingBean(
            GetCurrentSessionUseCase.class
    )
    GetCurrentSessionUseCase
    getCurrentSessionUseCase(
            CurrentUserProvider
                    currentUserProvider
    ) {
        return new CurrentSessionService(
                currentUserProvider
        );
    }

    @Bean
    @ConditionalOnMissingBean(
            SecurityContextRepository.class
    )
    SecurityContextRepository
    securityContextRepository() {
        return new DelegatingSecurityContextRepository(
                new RequestAttributeSecurityContextRepository(),
                new HttpSessionSecurityContextRepository()
        );
    }

    @Bean
    @ConditionalOnMissingBean(
            CsrfTokenRepository.class
    )
    CsrfTokenRepository
    csrfTokenRepository() {
        CookieCsrfTokenRepository repository =
                CookieCsrfTokenRepository
                        .withHttpOnlyFalse();

        repository.setCookieCustomizer(
                cookie ->
                        cookie.path("/")
                                .sameSite("Strict")
        );

        return repository;
    }

    @Bean
    @ConditionalOnMissingBean(
            SpringSecuritySessionManager.class
    )
    SpringSecuritySessionManager
    securitySessionManager(
            SecurityContextRepository
                    securityContextRepository,
            CsrfTokenRepository
                    csrfTokenRepository
    ) {
        return new SpringSecuritySessionManager(
                securityContextRepository,
                csrfTokenRepository
        );
    }

    @Bean
    @ConditionalOnMissingBean(
            RestrictedLocalSessionFilter.class
    )
    RestrictedLocalSessionFilter
    restrictedLocalSessionFilter(
            SpringSecuritySessionManager
                    sessionManager
    ) {
        return new RestrictedLocalSessionFilter(
                sessionManager
        );
    }

    @Bean
    @ConditionalOnMissingBean(
            SecurityFilterChain.class
    )
    SecurityFilterChain
    securityFilterChain(
            HttpSecurity http,
            ObjectProvider<OidcAuthenticationAdapter>
                    oidcAuthenticationAdapterProvider,
            SecurityContextRepository
                    securityContextRepository,
            CsrfTokenRepository
                    csrfTokenRepository,
            AuthenticationCapabilitiesProperties
                    capabilities,
            SecurityAuditPort
                    securityAuditPort,
            RestrictedLocalSessionFilter
                    restrictedLocalSessionFilter
    ) throws Exception {

        RequestMatcher bearerRequest =
                request -> {
                    String authorization =
                            request.getHeader(
                                    HttpHeaders.AUTHORIZATION
                            );

                    return authorization != null
                            && authorization
                            .startsWith(
                                    "Bearer "
                            );
                };

        AuditingAuthenticationEntryPoint
                auditingAuthenticationEntryPoint =
                new AuditingAuthenticationEntryPoint(
                        securityAuditPort
                );

        http
                .httpBasic(
                        AbstractHttpConfigurer::disable
                )
                .formLogin(
                        AbstractHttpConfigurer::disable
                )
                .exceptionHandling(
                        exceptions ->
                                exceptions
                                        .authenticationEntryPoint(
                                                auditingAuthenticationEntryPoint
                                        )
                )
                .securityContext(
                        context ->
                                context
                                        .securityContextRepository(
                                                securityContextRepository
                                        )
                )
                .sessionManagement(
                        session ->
                                session
                                        .sessionCreationPolicy(
                                                SessionCreationPolicy.IF_REQUIRED
                                        )
                )
                .csrf(
                        csrf -> {
                            csrf.csrfTokenRepository(
                                    csrfTokenRepository
                            );

                            /*
                             * SIXPAY is an Angular SPA. Angular sends the raw
                             * XSRF-TOKEN cookie value in X-XSRF-TOKEN.
                             *
                             * Spring Security's default XOR request handler
                             * expects a BREACH-encoded request token and
                             * therefore rejects the otherwise identical raw
                             * cookie/header pair as an invalid CSRF token.
                             *
                             * Use the plain request-attribute handler for the
                             * SPA contract while retaining CookieCsrfTokenRepository.
                             */
                            csrf.csrfTokenRequestHandler(
                                    new CsrfTokenRequestAttributeHandler()
                            );

                            csrf.ignoringRequestMatchers(
                                    bearerRequest
                            );

                            if (capabilities
                                    .localEnabled()) {
                                csrf.ignoringRequestMatchers(
                                        request ->
                                                HttpMethod.POST
                                                        .matches(
                                                                request.getMethod()
                                                        )
                                                        && "/api/v1/auth/login"
                                                        .equals(
                                                                request.getRequestURI()
                                                        )
                                );
                            }
                        }
                )
                .authorizeHttpRequests(
                        authorize -> {
                            authorize
                                    .requestMatchers(
                                            "/actuator/health",
                                            "/actuator/health/**"
                                    )
                                    .permitAll();

                            if (capabilities
                                    .localEnabled()) {
                                authorize
                                        .requestMatchers(
                                                HttpMethod.POST,
                                                "/api/v1/auth/login"
                                        )
                                        .permitAll();
                            }

                            authorize
                                    .anyRequest()
                                    .authenticated();
                        }
                )
                /*
                 * ExceptionTranslationFilter must wrap the restricted-session
                 * filter so AccessDeniedException is translated consistently
                 * by Spring Security. AuthorizationFilter remains downstream.
                 */
                .addFilterAfter(
                        restrictedLocalSessionFilter,
                        ExceptionTranslationFilter.class
                );

        if (capabilities
                .oidcEnabled()) {
            http.oauth2ResourceServer(
                    oauth2 ->
                            oauth2
                                    /*
                                     * Resource Server authentication failures
                                     * happen inside BearerTokenAuthenticationFilter
                                     * and use the resource-server entry point,
                                     * not only exceptionHandling().
                                     *
                                     * Explicitly route them through SIXPAY's
                                     * auditing entry point.
                                     */
                                    .authenticationEntryPoint(
                                            auditingAuthenticationEntryPoint
                                    )
                                    .jwt(
                                            jwt ->
                                                    jwt.jwtAuthenticationConverter(
                                                            oidcAuthenticationAdapterProvider
                                                                    .getObject()
                                                    )
                                    )
            );
        }

        return http.build();
    }
}
