package com.sixpay.payment.infrastructure.observability;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Establishes a safe correlation ID for Payment HTTP logs and responses.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public final class PaymentCorrelationLoggingFilter
        extends OncePerRequestFilter {

    static final String CORRELATION_HEADER =
            "X-Correlation-ID";
    static final String MDC_CORRELATION_KEY =
            "correlationId";

    @Override
    protected boolean shouldNotFilter(
            HttpServletRequest request
    ) {
        return !request.getRequestURI()
                .startsWith("/internal/api/v1/payments");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String correlationId = normalize(
                request.getHeader(CORRELATION_HEADER)
        );

        MDC.put(
                MDC_CORRELATION_KEY,
                correlationId
        );
        response.setHeader(
                CORRELATION_HEADER,
                correlationId
        );

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_CORRELATION_KEY);
        }
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return UUID.randomUUID().toString();
        }

        try {
            return UUID.fromString(value).toString();
        } catch (IllegalArgumentException exception) {
            return UUID.randomUUID().toString();
        }
    }
}
