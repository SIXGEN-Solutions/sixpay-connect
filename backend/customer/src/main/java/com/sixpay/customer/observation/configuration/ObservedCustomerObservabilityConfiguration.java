package com.sixpay.customer.observation.configuration;

import org.springframework.boot.autoconfigure.condition
        .ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Provides shared infrastructure dependencies required by Customer
 * Observation auditing, queries, metrics and health indicators.
 */
@Configuration(proxyBeanMethods = false)
public class ObservedCustomerObservabilityConfiguration {

    public static final String OBSERVED_CUSTOMER_CLOCK =
            "observedCustomerClock";

    @Bean(name = OBSERVED_CUSTOMER_CLOCK)
    @ConditionalOnMissingBean(
            name = OBSERVED_CUSTOMER_CLOCK
    )
    Clock observedCustomerClock() {
        return Clock.systemUTC();
    }
}
