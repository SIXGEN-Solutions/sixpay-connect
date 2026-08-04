package com.sixpay.bootstrap.integration.customer;

import com.sixpay.customer.observation.application.port.input.ObserveCustomerUseCase;
import com.sixpay.payment.application.port.output.ObservedCustomerProjectionPort;
import com.sixpay.payment.application.port.output.PaymentLookupPort;
import com.sixpay.payment.application.service.PaymentObservedCustomerProjectionRequestFactory;
import com.sixpay.payment.application.service.PaymentObservedCustomerProjectionService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnBean(ObserveCustomerUseCase.class)
public class ObservedCustomerProjectionIntegrationConfiguration {

    @Bean
    ObservedCustomerProjectionPort observedCustomerProjectionPort(
            ObserveCustomerUseCase useCase
    ) {
        return new ObservedCustomerProjectionModuleAdapter(
                useCase
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
                    PaymentLookupPort paymentLookupPort,
                    ObservedCustomerProjectionPort projectionPort,
                    PaymentObservedCustomerProjectionRequestFactory factory
            ) {
        return new PaymentObservedCustomerProjectionService(
                paymentLookupPort,
                projectionPort,
                factory
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
