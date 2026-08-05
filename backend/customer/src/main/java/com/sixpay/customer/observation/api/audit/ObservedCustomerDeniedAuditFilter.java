package com.sixpay.customer.observation.api.audit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Objects;

/**
 * Audits authorization denials that occur before controller invocation.
 */
public final class ObservedCustomerDeniedAuditFilter
        extends OncePerRequestFilter {

    private static final String API_PREFIX =
            "/internal/api/v1/observed-customers";

    private static final String CORRELATION_HEADER =
            "X-Correlation-ID";

    private final ObservedCustomerQueryAuditTrail auditTrail;

    public ObservedCustomerDeniedAuditFilter(
            ObservedCustomerQueryAuditTrail auditTrail
    ) {
        this.auditTrail = Objects.requireNonNull(auditTrail);
    }

    @Override
    protected boolean shouldNotFilter(
            HttpServletRequest request
    ) {
        return !request.getRequestURI()
                .startsWith(API_PREFIX);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        filterChain.doFilter(request, response);

        if (response.getStatus()
                == HttpServletResponse.SC_FORBIDDEN) {
            auditTrail.denied(
                    request.getHeader(CORRELATION_HEADER)
            );
        }
    }
}
