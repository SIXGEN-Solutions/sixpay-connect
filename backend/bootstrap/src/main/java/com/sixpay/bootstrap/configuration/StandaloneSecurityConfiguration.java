package com.sixpay.bootstrap.configuration;

import com.sixpay.security.authorization.SixpayRole;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Configuration(proxyBeanMethods = false)
@Profile("standalone")
public class StandaloneSecurityConfiguration {

    private static final String LOCAL_SUBJECT =
            "00000000-0000-0000-0000-000000000001";

    @Bean
    SecurityFilterChain standaloneSecurityFilterChain(
            HttpSecurity http
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
                                        "/actuator/health/**",
                                        "/v3/api-docs/**",
                                        "/swagger-ui.html",
                                        "/swagger-ui/**"
                                )
                                .permitAll()
                                .anyRequest()
                                .authenticated()
                )

                .addFilterBefore(
                        new StandaloneAuthenticationFilter(),
                        AnonymousAuthenticationFilter.class
                );

        return http.build();
    }

    private static final class StandaloneAuthenticationFilter
            extends OncePerRequestFilter {

        private static final List<SimpleGrantedAuthority> AUTHORITIES =
                List.of(
                        new SimpleGrantedAuthority(
                                SixpayRole.ADMIN.authority()
                        ),
                        new SimpleGrantedAuthority(
                                SixpayRole.MANAGER.authority()
                        ),
                        new SimpleGrantedAuthority(
                                SixpayRole.AUDITOR.authority()
                        )
                );

        @Override
        protected void doFilterInternal(
                HttpServletRequest request,
                HttpServletResponse response,
                FilterChain filterChain
        ) throws ServletException, IOException {

            if (SecurityContextHolder.getContext()
                    .getAuthentication() == null) {

                var authentication =
                        UsernamePasswordAuthenticationToken.authenticated(
                                LOCAL_SUBJECT,
                                null,
                                AUTHORITIES
                        );

                var securityContext =
                        SecurityContextHolder.createEmptyContext();

                securityContext.setAuthentication(authentication);
                SecurityContextHolder.setContext(securityContext);
            }

            filterChain.doFilter(request, response);
        }
    }
}