package com.sixpay.customer.observation.configuration;

import com.sixpay.customer.observation.infrastructure.observability
        .ObservedCustomerProjectionMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.condition
        .ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition
        .ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration(proxyBeanMethods = false)
@ConditionalOnBean(MeterRegistry.class)
public class ObservedCustomerProjectionObservabilityConfiguration {

    @Bean
    @ConditionalOnMissingBean(Clock.class)
    Clock observedCustomerProjectionClock() {
        return Clock.systemUTC();
    }

    @Bean
    ObservedCustomerProjectionMetrics
    observedCustomerProjectionMetrics(
            MeterRegistry meterRegistry
    ) {
        return new ObservedCustomerProjectionMetrics(
                meterRegistry
        );
    }
}