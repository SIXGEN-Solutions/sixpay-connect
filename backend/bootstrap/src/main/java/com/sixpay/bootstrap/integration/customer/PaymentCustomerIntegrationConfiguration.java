package com.sixpay.bootstrap.integration.customer;

import com.sixpay.customer.verification.application.port.input.VerifyCustomerUseCase;
import com.sixpay.payment.application.port.output.CustomerVerificationPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class PaymentCustomerIntegrationConfiguration {

    @Bean
    @ConditionalOnBean(com.sixpay.customer.management.application.port.input.ResolveCustomerSubscriptionUseCase.class)
    com.sixpay.payment.application.port.output.customer.PaymentCustomerSubscriptionPort
    paymentCustomerSubscriptionPort(
            com.sixpay.customer.management.application.port.input.ResolveCustomerSubscriptionUseCase resolver) {
        return new PaymentCustomerSubscriptionModuleAdapter(resolver);
    }

    @Bean
    @ConditionalOnBean(VerifyCustomerUseCase.class)
    CustomerVerificationPort customerVerificationPort(
            VerifyCustomerUseCase verifyCustomerUseCase
    ) {
        return new CustomerVerificationModuleAdapter(
                verifyCustomerUseCase
        );
    }
}
