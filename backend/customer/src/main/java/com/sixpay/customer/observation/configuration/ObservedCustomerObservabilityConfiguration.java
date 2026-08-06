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

    @Bean
    @ConditionalOnMissingBean(Clock.class)
    Clock observedCustomerClock() {
        return Clock.systemUTC();
    }
}