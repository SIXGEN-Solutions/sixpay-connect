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
        .ConditionalOnBean;
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
public class ObservedCustomerQueryApiConfiguration {

    @Bean
    @ConditionalOnBean({
            ObservedCustomerAuditPort.class,
            ObservedCustomerAuditIdGenerator.class,
            MeterRegistry.class,
            Clock.class
    })
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
    @ConditionalOnBean(ObservedCustomerQueryAuditTrail.class)
    ObservedCustomerDeniedAuditFilter
    observedCustomerDeniedAuditFilter(
            ObservedCustomerQueryAuditTrail auditTrail
    ) {
        return new ObservedCustomerDeniedAuditFilter(
                auditTrail
        );
    }

    @Bean
    @ConditionalOnBean({
            MeterRegistry.class,
            Clock.class,
            ObservedCustomerQueryAuditTrail.class
    })
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
