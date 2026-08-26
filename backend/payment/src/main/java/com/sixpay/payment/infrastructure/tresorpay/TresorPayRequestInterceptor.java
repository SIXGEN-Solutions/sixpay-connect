package com.sixpay.payment.infrastructure.tresorpay;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Objects;

public final class TresorPayRequestInterceptor
        implements HandlerInterceptor {

    private final TresorPayRequestGuard requestGuard;

    public TresorPayRequestInterceptor(
            TresorPayRequestGuard requestGuard
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
