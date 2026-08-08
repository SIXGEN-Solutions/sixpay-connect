package com.sixpay.reporting.api.observability;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Objects;

public final class PaymentAuditHttpObservationInterceptor
        implements HandlerInterceptor {

    private static final String SAMPLE_ATTRIBUTE =
            PaymentAuditHttpObservationInterceptor.class.getName()
                    + ".sample";

    private final MeterRegistry meterRegistry;

    public PaymentAuditHttpObservationInterceptor(
            MeterRegistry meterRegistry
    ) {
        this.meterRegistry = Objects.requireNonNull(
                meterRegistry,
                "meterRegistry is required"
        );
    }

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler
    ) {
        request.setAttribute(
                SAMPLE_ATTRIBUTE,
                Timer.start(meterRegistry)
        );
        return true;
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception exception
    ) {
        Object value = request.getAttribute(SAMPLE_ATTRIBUTE);
        if (!(value instanceof Timer.Sample sample)) {
            return;
        }

        sample.stop(
                Timer.builder("sixpay.reporting.audit.http")
                        .description(
                                "Internal Payment audit HTTP latency"
                        )
                        .tag("operation", operation(request))
                        .tag(
                                "outcome",
                                outcome(response.getStatus())
                        )
                        .tag(
                                "status",
                                Integer.toString(
                                        response.getStatus()
                                )
                        )
                        .register(meterRegistry)
        );
    }

    private static String operation(
            HttpServletRequest request
    ) {
        String uri = request.getRequestURI();
        String method = request.getMethod();

        if (uri.matches(
                ".*/payments/[^/]+/timeline$"
        )) {
            return "payment_timeline";
        }

        if (uri.endsWith(
                "/payment-audit-records"
        )) {
            return "payment_audit_search";
        }

        if (uri.matches(
                ".*/payment-audit-records/[^/]+$"
        )) {
            return "payment_audit_detail";
        }

        if (uri.endsWith(
                "/payment-audit-exports"
        ) && "POST".equals(method)) {
            return "payment_audit_export_request";
        }

        if (uri.matches(
                ".*/payment-audit-exports/[^/]+$"
        )) {
            return "payment_audit_export_status";
        }

        return "payment_audit_other";
    }

    private static String outcome(int status) {
        if (status >= 200 && status < 300) {
            return "success";
        }
        if (status >= 400 && status < 500) {
            return "client_error";
        }
        if (status >= 500) {
            return "server_error";
        }
        return "other";
    }
}
