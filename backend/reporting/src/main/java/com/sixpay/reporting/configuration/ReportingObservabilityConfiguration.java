package com.sixpay.reporting.configuration;

import com.sixpay.reporting.api.observability
        .PaymentAuditHttpObservationInterceptor;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation
        .InterceptorRegistry;
import org.springframework.web.servlet.config.annotation
        .WebMvcConfigurer;

@Configuration(proxyBeanMethods = false)
public class ReportingObservabilityConfiguration
        implements WebMvcConfigurer {

    private final PaymentAuditHttpObservationInterceptor interceptor;

    public ReportingObservabilityConfiguration(
            MeterRegistry meterRegistry
    ) {
        this.interceptor =
                new PaymentAuditHttpObservationInterceptor(
                        meterRegistry
                );
    }

    @Override
    public void addInterceptors(
            InterceptorRegistry registry
    ) {
        registry.addInterceptor(interceptor)
                .addPathPatterns(
                        "/internal/api/v1/payments/*/timeline",
                        "/internal/api/v1/payment-audit-records",
                        "/internal/api/v1/payment-audit-records/*",
                        "/internal/api/v1/payment-audit-exports",
                        "/internal/api/v1/payment-audit-exports/*"
                );
    }
}
