package com.sixpay.customer.observation.configuration;

import com.sixpay.customer.observation.infrastructure.observability
        .ObservedCustomerProjectionMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.condition
        .ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configures Customer Observation projection metrics when Micrometer
 * is available.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnBean(MeterRegistry.class)
public class ObservedCustomerProjectionObservabilityConfiguration {

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