package com.sixpay.payment.infrastructure.partner;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Objects;

public final class PartnerRequestInterceptor
        implements HandlerInterceptor {

    private final PartnerRequestGuard requestGuard;

    public PartnerRequestInterceptor(
            PartnerRequestGuard requestGuard
    ) {
        this.requestGuard = Objects.requireNonNull(requestGuard);
    }

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler
    ) {
        requestGuard.validateTransportRequest(request);
        return true;
    }
}
