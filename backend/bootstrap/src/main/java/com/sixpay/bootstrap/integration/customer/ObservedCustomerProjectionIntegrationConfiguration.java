package com.sixpay.bootstrap.integration.customer;

import com.sixpay.bootstrap.integration.customer.mapper
        .PaymentProjectionEventCommandMapper;
import com.sixpay.customer.observation.application.port.input
        .ObserveCustomerUseCase;
import com.sixpay.payment.application.port.output
        .ObservedCustomerProjectionPort;
import com.sixpay.payment.application.port.output.query
        .PaymentObservedCustomerLinkPort;
import com.sixpay.payment.application.service
        .PaymentObservedCustomerProjectionRequestFactory;
import com.sixpay.payment.application.service
        .PaymentObservedCustomerProjectionService;
import org.springframework.boot.autoconfigure.condition
        .ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
        prefix = "sixpay.customer.observation.persistence",
        name = "enabled",
        havingValue = "true"
)
public class ObservedCustomerProjectionIntegrationConfiguration {

    @Bean
    PaymentProjectionEventCommandMapper
    paymentProjectionEventCommandMapper() {

        return new PaymentProjectionEventCommandMapper();
    }

    @Bean
    ObservedCustomerProjectionPort observedCustomerProjectionPort(
            ObserveCustomerUseCase useCase,
            PaymentProjectionEventCommandMapper commandMapper
    ) {
        return new ObservedCustomerProjectionModuleAdapter(
                useCase,
                commandMapper
        );
    }

    @Bean
    PaymentObservedCustomerProjectionRequestFactory
    paymentObservedCustomerProjectionRequestFactory() {

        return new PaymentObservedCustomerProjectionRequestFactory();
    }

    @Bean
    PaymentObservedCustomerProjectionService
    paymentObservedCustomerProjectionService(
            ObservedCustomerProjectionPort projectionPort,
            PaymentObservedCustomerProjectionRequestFactory
                    requestFactory,
            PaymentObservedCustomerLinkPort linkPort
    ) {
        return new PaymentObservedCustomerProjectionService(
                projectionPort,
                requestFactory,
                linkPort
        );
    }

    @Bean
    PaymentObservedCustomerOutboxConsumer
    paymentObservedCustomerOutboxConsumer(
            PaymentObservedCustomerProjectionService service
    ) {
        return new PaymentObservedCustomerOutboxConsumer(
                service
        );
    }
}