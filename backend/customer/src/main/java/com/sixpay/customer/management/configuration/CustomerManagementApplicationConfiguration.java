package com.sixpay.customer.management.configuration;

import com.sixpay.customer.management.application.port.output.CustomerEnrollmentIdGenerator;
import com.sixpay.customer.management.application.port.output.CustomerEnrollmentTimeProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Instant;
import java.util.UUID;

/**
 * Environment-neutral runtime wiring for Customer Management.
 *
 * <p>Customer enrollment uses explicit output ports for time and identifier
 * generation so the application layer remains framework-free and testable.
 * The standard runtime implementations are valid for every environment.</p>
 */
@Configuration(proxyBeanMethods = false)
public class CustomerManagementApplicationConfiguration {

    @Bean
    @ConditionalOnMissingBean(CustomerEnrollmentIdGenerator.class)
    CustomerEnrollmentIdGenerator customerEnrollmentIdGenerator() {
        return UUID::randomUUID;
    }

    @Bean
    @ConditionalOnMissingBean(CustomerEnrollmentTimeProvider.class)
    CustomerEnrollmentTimeProvider customerEnrollmentTimeProvider() {
        return Instant::now;
    }
}
