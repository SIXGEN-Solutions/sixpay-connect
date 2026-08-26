package com.sixpay.payment.infrastructure.tresorpay;

import com.sixpay.integration.http.IntegrationHttpHeaders;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public final class TresorPayRequestGuardFilter
        extends OncePerRequestFilter {

    private static final String PAYMENT_INITIATION_PATH =
            "/v1/payments/initiate";

    private final TresorPayRequestGuard requestGuard;

    public TresorPayRequestGuardFilter(
            TresorPayRequestGuard requestGuard
    ) {
        this.requestGuard = requestGuard;
    }

    @Override
    protected boolean shouldNotFilter(
            HttpServletRequest request
    ) {
        return !PAYMENT_INITIATION_PATH.equals(
                request.getRequestURI()
        );
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        requestGuard.validateTransportRequest(request);

        filterChain.doFilter(request, response);
    }
}