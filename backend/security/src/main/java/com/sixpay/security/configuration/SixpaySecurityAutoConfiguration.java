package com.sixpay.security.configuration;

import com.sixpay.security.authentication.CurrentUserProvider;
import com.sixpay.security.authentication
        .SecurityContextCurrentUserProvider;
import com.sixpay.security.jwt.SixpayJwtAuthoritiesConverter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition
        .ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition
        .ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition
        .ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.method.configuration
        .EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders
        .HttpSecurity;
import org.springframework.security.config.annotation.web.configurers
        .AbstractHttpConfigurer;
import org.springframework.security.config.http
        .SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource
        .authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * Default Spring Security configuration for SIXPAY CONNECT.
 */
@AutoConfiguration
@EnableMethodSecurity
@ConditionalOnClass({
        HttpSecurity.class,
        Jwt.class
})
@ConditionalOnWebApplication(
        type = ConditionalOnWebApplication.Type.SERVLET
)
public class SixpaySecurityAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    SixpayJwtAuthoritiesConverter
    sixpayJwtAuthoritiesConverter() {
        return new SixpayJwtAuthoritiesConverter();
    }

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
    @ConditionalOnMissingBean(CurrentUserProvider.class)
    CurrentUserProvider currentUserProvider() {
        return new SecurityContextCurrentUserProvider();
    }

    @Bean
    @ConditionalOnMissingBean(SecurityFilterChain.class)
    @ConditionalOnProperty(
            prefix = "sixpay.security",
            name = "mode",
            havingValue = "oauth2",
            matchIfMissing = true
    )
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationConverter jwtAuthenticationConverter
    ) throws Exception {

        http
                .csrf(AbstractHttpConfigurer::disable)

                .httpBasic(AbstractHttpConfigurer::disable)

                .formLogin(AbstractHttpConfigurer::disable)

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .authorizeHttpRequests(authorize ->
                        authorize
                                .requestMatchers(
                                        "/actuator/health",
                                        "/actuator/health/**"
                                )
                                .permitAll()

                                .anyRequest()
                                .authenticated()
                )

                .oauth2ResourceServer(oauth2 ->
                        oauth2.jwt(jwt ->
                                jwt.jwtAuthenticationConverter(
                                        jwtAuthenticationConverter
                                )
                        )
                );

        return http.build();
    }
}