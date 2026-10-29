package com.sixpay.security.infrastructure.authentication.subscription;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Objects;
import java.util.function.Predicate;

public final class TresorPaySubscriptionKeyFilter extends OncePerRequestFilter {

    public static final String HEADER_NAME = "X-Subscription-Key";
    private static final String PREFIX = "/api/v1/integrations/tresorpay/payments";

    private final Predicate<String> validator;
    private final AuthenticationEntryPoint authenticationEntryPoint;

    public TresorPaySubscriptionKeyFilter(
            Predicate<String> validator,
            AuthenticationEntryPoint authenticationEntryPoint
    ) {
        this.validator = Objects.requireNonNull(validator, "Subscription key validator");
        this.authenticationEntryPoint = Objects.requireNonNull(
                authenticationEntryPoint,
                "Authentication entry point"
        );
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri == null || !uri.startsWith(PREFIX);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String key = request.getHeader(HEADER_NAME);
        if (key == null || key.isBlank() || !validator.test(key)) {
            authenticationEntryPoint.commence(
                    request,
                    response,
                    new BadCredentialsException("Invalid TRESOR PAY subscription key")
            );
            return;
        }
        filterChain.doFilter(request, response);
    }
}
