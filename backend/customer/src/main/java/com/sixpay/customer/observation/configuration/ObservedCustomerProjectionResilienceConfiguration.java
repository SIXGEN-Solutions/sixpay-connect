package com.sixpay.customer.observation.configuration;

import com.sixpay.customer.observation.infrastructure.resilience
        .LockSupportObservedCustomerProjectionBackoff;
import com.sixpay.customer.observation.infrastructure.resilience
        .ObservedCustomerProjectionBackoff;
import com.sixpay.customer.observation.infrastructure.resilience
        .ObservedCustomerProjectionFailureClassifier;
import com.sixpay.customer.observation.infrastructure.resilience
        .ObservedCustomerProjectionRetryPolicy;
import org.springframework.boot.autoconfigure.condition
        .ConditionalOnMissingBean;
import org.springframework.boot.context.properties
        .EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ThreadLocalRandom;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(
        ObservedCustomerProjectionResilienceProperties.class
)
public class ObservedCustomerProjectionResilienceConfiguration {

    @Bean
    @ConditionalOnMissingBean
    ObservedCustomerProjectionFailureClassifier
    observedCustomerProjectionFailureClassifier() {
        return new ObservedCustomerProjectionFailureClassifier();
    }

    @Bean
    @ConditionalOnMissingBean
    ObservedCustomerProjectionBackoff
    observedCustomerProjectionBackoff() {
        return new LockSupportObservedCustomerProjectionBackoff();
    }

    @Bean
    @ConditionalOnMissingBean
    ObservedCustomerProjectionRetryPolicy
    observedCustomerProjectionRetryPolicy(
            ObservedCustomerProjectionResilienceProperties properties,
            ObservedCustomerProjectionBackoff backoff
    ) {
        return new ObservedCustomerProjectionRetryPolicy(
                properties.maxAttempts(),
                properties.initialBackoff(),
                properties.maxBackoff(),
                properties.multiplier(),
                properties.jitter(),
                backoff,
                () -> ThreadLocalRandom.current().nextDouble()
        );
    }
}