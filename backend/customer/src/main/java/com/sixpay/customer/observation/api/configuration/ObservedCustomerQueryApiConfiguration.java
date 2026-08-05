package com.sixpay.customer.observation.api.configuration;

import com.sixpay.customer.observation.api.observability
        .ObservedCustomerQueryObservation;
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
            MeterRegistry.class,
            Clock.class
    })
    ObservedCustomerQueryObservation
    observedCustomerQueryObservation(
            MeterRegistry meterRegistry,
            Clock clock
    ) {
        return new ObservedCustomerQueryObservation(
                meterRegistry,
                clock
        );
    }
}
