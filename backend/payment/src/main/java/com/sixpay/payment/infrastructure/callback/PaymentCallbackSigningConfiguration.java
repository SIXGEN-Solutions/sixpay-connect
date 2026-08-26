package com.sixpay.payment.infrastructure.callback;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(
        prefix = "sixpay.payment.callback",
        name = "enabled",
        havingValue = "true"
)
public class PaymentCallbackSigningConfiguration {

    @Bean
    @ConditionalOnMissingBean
    CallbackSigningKeyProvider callbackSigningKeyProvider(
            PaymentCallbackProperties properties
    ) {
        return new PemCallbackSigningKeyProvider(properties);
    }
}
