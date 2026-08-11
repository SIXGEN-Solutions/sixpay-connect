package com.sixpay.security.configuration;

import com.sixpay.security.application.port.out.ExternalIdentityResolver;
import com.sixpay.security.application.service.SubjectExternalIdentityResolver;
import com.sixpay.security.authentication.CurrentUserProvider;
import com.sixpay.security.authentication.SecurityContextCurrentUserProvider;
import com.sixpay.security.infrastructure.authentication.oidc.OidcAuthenticationAdapter;
import com.sixpay.security.jwt.SixpayJwtAuthoritiesConverter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.context.DelegatingSecurityContextRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.util.matcher.RequestMatcher;

@AutoConfiguration
@EnableMethodSecurity
@EnableConfigurationProperties(AuthenticationCapabilitiesProperties.class)
@Import(LocalAuthenticationConfiguration.class)
@ConditionalOnClass({HttpSecurity.class, Jwt.class})
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class SixpaySecurityAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    SixpayJwtAuthoritiesConverter sixpayJwtAuthoritiesConverter() {
        return new SixpayJwtAuthoritiesConverter();
    }

    /**
     * Retained as a compatibility bean for existing consumers/tests.
     *
     * <p>The Resource Server itself uses {@link OidcAuthenticationAdapter}
     * once the OIDC capability is enabled.</p>
     */
    @Bean
    @ConditionalOnMissingBean(JwtAuthenticationConverter.class)
    JwtAuthenticationConverter jwtAuthenticationConverter(
            SixpayJwtAuthoritiesConverter authoritiesConverter
    ) {
        JwtAuthenticationConverter converter =
                new JwtAuthenticationConverter();

        converter.setJwtGrantedAuthoritiesConverter(
                authoritiesConverter
        );

        return converter;
    }

    @Bean
    @ConditionalOnMissingBean(ExternalIdentityResolver.class)
    ExternalIdentityResolver externalIdentityResolver() {
        return new SubjectExternalIdentityResolver();
    }

    @Bean
    @ConditionalOnMissingBean(OidcAuthenticationAdapter.class)
    OidcAuthenticationAdapter oidcAuthenticationAdapter(
            SixpayJwtAuthoritiesConverter authoritiesConverter,
            ExternalIdentityResolver externalIdentityResolver
    ) {
        return new OidcAuthenticationAdapter(
                authoritiesConverter,
                externalIdentityResolver
        );
    }

    @Bean
    @ConditionalOnMissingBean(CurrentUserProvider.class)
    CurrentUserProvider currentUserProvider() {
        return new SecurityContextCurrentUserProvider();
    }

    @Bean
    @ConditionalOnMissingBean(SecurityContextRepository.class)
    SecurityContextRepository securityContextRepository() {
        return new DelegatingSecurityContextRepository(
                new RequestAttributeSecurityContextRepository(),
                new HttpSessionSecurityContextRepository()
        );
    }

    @Bean
    @ConditionalOnMissingBean(CsrfTokenRepository.class)
    CsrfTokenRepository csrfTokenRepository() {
        CookieCsrfTokenRepository repository =
                CookieCsrfTokenRepository.withHttpOnlyFalse();

        repository.setCookieCustomizer(cookie ->
                cookie
                        .path("/")
                        .sameSite("Strict")
        );

        return repository;
    }

    @Bean
    @ConditionalOnMissingBean(SecurityFilterChain.class)
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            OidcAuthenticationAdapter oidcAuthenticationAdapter,
            SecurityContextRepository securityContextRepository,
            CsrfTokenRepository csrfTokenRepository,
            AuthenticationCapabilitiesProperties capabilities
    ) throws Exception {

        RequestMatcher bearerRequest = request -> {
            String authorization =
                    request.getHeader(HttpHeaders.AUTHORIZATION);

            return authorization != null
                    && authorization.startsWith("Bearer ");
        };

        http
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)

                .exceptionHandling(exceptions ->
                        exceptions.authenticationEntryPoint(
                                new HttpStatusEntryPoint(
                                        HttpStatus.UNAUTHORIZED
                                )
                        )
                )

                .securityContext(context ->
                        context.securityContextRepository(
                                securityContextRepository
                        )
                )

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.IF_REQUIRED
                        )
                )

                .csrf(csrf -> {
                    csrf.csrfTokenRepository(
                            csrfTokenRepository
                    );

                    csrf.ignoringRequestMatchers(
                            bearerRequest
                    );

                    if (capabilities.localEnabled()) {
                        csrf.ignoringRequestMatchers(
                                request ->
                                        HttpMethod.POST.matches(
                                                request.getMethod()
                                        )
                                                && "/api/v1/auth/login"
                                                .equals(
                                                        request.getRequestURI()
                                                )
                        );
                    }
                })

                .authorizeHttpRequests(authorize -> {
                    authorize
                            .requestMatchers(
                                    "/actuator/health",
                                    "/actuator/health/**"
                            )
                            .permitAll();

                    if (capabilities.localEnabled()) {
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
                });

        if (capabilities.oidcEnabled()) {
            http.oauth2ResourceServer(oauth2 ->
                    oauth2.jwt(jwt ->
                            jwt.jwtAuthenticationConverter(
                                    oidcAuthenticationAdapter
                            )
                    )
            );
        }

        return http.build();
    }
}
