package com.sixpay.payment.infrastructure.partner;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.Clock;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(
        PartnerIntegrationProperties.class
)
@ConditionalOnProperty(
        prefix = "sixpay.payment.partner",
        name = "enabled",
        havingValue = "true"
)
public class PartnerIntegrationConfiguration
        implements WebMvcConfigurer {

    private final PartnerRequestInterceptor interceptor;

    public PartnerIntegrationConfiguration(
            PartnerRequestInterceptor interceptor
    ) {
        this.interceptor = interceptor;
    }

    @Override
    public void addInterceptors(
            InterceptorRegistry registry
    ) {
        registry.addInterceptor(interceptor)
                .addPathPatterns(
                        "/api/v1/partners/payments"
                );
    }

    @Bean
    static Clock partnerClock() {
        return Clock.systemUTC();
    }

    @Bean
    static PartnerNonceStore partnerNonceStore(
            Clock partnerClock
    ) {
        return new InMemoryPartnerNonceStore(
                partnerClock
        );
    }

    @Bean
    static PartnerRateLimiter partnerRateLimiter(
            PartnerIntegrationProperties properties,
            Clock partnerClock
    ) {
        return new FixedWindowPartnerRateLimiter(
                partnerClock,
                properties.rateLimit().requestsPerMinute()
        );
    }

    @Bean
    static StructuredPartnerAccessAudit partnerAccessAudit() {
        return new StructuredPartnerAccessAudit();
    }

    @Bean
    static PartnerRequestGuard partnerRequestGuard(
            PartnerIntegrationProperties properties,
            PartnerNonceStore nonceStore,
            PartnerRateLimiter rateLimiter,
            StructuredPartnerAccessAudit audit,
            Clock partnerClock
    ) {
        return new PartnerRequestGuard(
                properties,
                nonceStore,
                rateLimiter,
                audit,
                partnerClock
        );
    }

    @Bean
    static PartnerRequestInterceptor partnerRequestInterceptor(
            PartnerRequestGuard requestGuard
    ) {
        return new PartnerRequestInterceptor(
                requestGuard
        );
    }
}
