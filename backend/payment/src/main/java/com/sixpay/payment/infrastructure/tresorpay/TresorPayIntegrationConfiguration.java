package com.sixpay.payment.infrastructure.tresorpay;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.Clock;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(
        TresorPayIntegrationProperties.class
)
@ConditionalOnProperty(
        prefix = "sixpay.payment.tresorpay",
        name = "enabled",
        havingValue = "true"
)
public class TresorPayIntegrationConfiguration
        implements WebMvcConfigurer {

    private final TresorPayRequestInterceptor interceptor;

    public TresorPayIntegrationConfiguration(
            TresorPayRequestInterceptor interceptor
    ) {
        this.interceptor = interceptor;
    }

    @Override
    public void addInterceptors(
            InterceptorRegistry registry
    ) {
        registry.addInterceptor(interceptor)
                .addPathPatterns(
                        "/v1/payments/initiate"
                );
    }

    @Bean
    static Clock tresorPayClock() {
        return Clock.systemUTC();
    }

    @Bean
    static TresorPayNonceStore tresorPayNonceStore(
            Clock tresorPayClock
    ) {
        return new InMemoryTresorPayNonceStore(
                tresorPayClock
        );
    }

    @Bean
    static TresorPayRateLimiter tresorPayRateLimiter(
            TresorPayIntegrationProperties properties,
            Clock tresorPayClock
    ) {
        return new FixedWindowTresorPayRateLimiter(
                tresorPayClock,
                properties.rateLimit().requestsPerMinute()
        );
    }

    @Bean
    static StructuredTresorPayAccessAudit tresorPayAccessAudit() {
        return new StructuredTresorPayAccessAudit();
    }

    @Bean
    static TresorPayRequestGuard tresorPayRequestGuard(
            TresorPayIntegrationProperties properties,
            TresorPayNonceStore nonceStore,
            TresorPayRateLimiter rateLimiter,
            StructuredTresorPayAccessAudit audit,
            Clock tresorPayClock
    ) {
        return new TresorPayRequestGuard(
                properties,
                nonceStore,
                rateLimiter,
                audit,
                tresorPayClock
        );
    }

    @Bean
    static TresorPayRequestInterceptor tresorPayRequestInterceptor(
            TresorPayRequestGuard requestGuard
    ) {
        return new TresorPayRequestInterceptor(
                requestGuard
        );
    }
}
