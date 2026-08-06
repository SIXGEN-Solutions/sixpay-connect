package com.sixpay.customer.observation.api.configuration;

import com.sixpay.customer.observation.api.audit
        .ObservedCustomerDeniedAuditFilter;
import com.sixpay.customer.observation.api.audit
        .ObservedCustomerQueryAuditTrail;
import com.sixpay.customer.observation.api.observability
        .ObservedCustomerQueryObservation;
import com.sixpay.customer.observation.application.port.output.audit
        .ObservedCustomerAuditIdGenerator;
import com.sixpay.customer.observation.application.port.output.audit
        .ObservedCustomerAuditPort;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.condition
        .ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition
        .ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration
        .EnableMethodSecurity;

import java.time.Clock;

@Configuration(proxyBeanMethods = false)
@EnableMethodSecurity
@ConditionalOnWebApplication(
        type = ConditionalOnWebApplication.Type.SERVLET
)
@ConditionalOnProperty(
        prefix = "sixpay.customer.observation.query",
        name = "enabled",
        havingValue = "true"
)
public class ObservedCustomerQueryApiConfiguration {

    @Bean
    ObservedCustomerQueryAuditTrail
    observedCustomerQueryAuditTrail(
            ObservedCustomerAuditPort auditPort,
            ObservedCustomerAuditIdGenerator auditIdGenerator,
            MeterRegistry meterRegistry,
            Clock clock
    ) {
        return new ObservedCustomerQueryAuditTrail(
                auditPort,
                auditIdGenerator,
                meterRegistry,
                clock
        );
    }

    @Bean
    ObservedCustomerDeniedAuditFilter
    observedCustomerDeniedAuditFilter(
            ObservedCustomerQueryAuditTrail auditTrail
    ) {
        return new ObservedCustomerDeniedAuditFilter(
                auditTrail
        );
    }

    @Bean
    ObservedCustomerQueryObservation
    observedCustomerQueryObservation(
            MeterRegistry meterRegistry,
            Clock clock,
            ObservedCustomerQueryAuditTrail auditTrail
    ) {
        return new ObservedCustomerQueryObservation(
                meterRegistry,
                clock,
                auditTrail
        );
    }
}