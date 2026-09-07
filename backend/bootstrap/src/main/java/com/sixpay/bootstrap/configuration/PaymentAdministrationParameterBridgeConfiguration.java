package com.sixpay.bootstrap.configuration;

import com.sixpay.administration.application.port.input.GeneralParameterQueryUseCase;
import com.sixpay.payment.application.port.output.configuration.PaymentConfigurationParameterPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class PaymentAdministrationParameterBridgeConfiguration {

    @Bean
    @ConditionalOnMissingBean(PaymentConfigurationParameterPort.class)
    PaymentConfigurationParameterPort paymentConfigurationParameterPort(
            GeneralParameterQueryUseCase parameters
    ) {
        return parameters::requireValue;
    }
}
