package com.sixpay.bootstrap.integration.customer;

import com.sixpay.customer.verification.application.port.input.VerifyCustomerUseCase;
import com.sixpay.payment.application.port.output.CustomerVerificationPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class PaymentCustomerIntegrationConfiguration {

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
