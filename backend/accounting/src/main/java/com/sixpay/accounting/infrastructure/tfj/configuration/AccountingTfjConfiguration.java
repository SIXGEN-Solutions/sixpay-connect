package com.sixpay.accounting.infrastructure.tfj.configuration;

import com.sixpay.accounting.configuration.AccountingModuleConfiguration;
import com.sixpay.accounting.infrastructure.tfj.mapper.AmplitudeTfjMapper;
import com.sixpay.accounting.infrastructure.tfj.security.AmplitudeTfjSignatureProperties;
import com.sixpay.accounting.infrastructure.tfj.security.AmplitudeTfjSignatureVerifier;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(
        AmplitudeTfjSignatureProperties.class
)
public class AccountingTfjConfiguration {

    @Bean
    AmplitudeTfjMapper amplitudeTfjMapper(
            @Qualifier(AccountingModuleConfiguration.ACCOUNTING_CLOCK)
            Clock clock
    ) {
        return new AmplitudeTfjMapper(clock);
    }

    @Bean
    AmplitudeTfjSignatureVerifier amplitudeTfjSignatureVerifier(
            AmplitudeTfjSignatureProperties properties,
            @Qualifier(AccountingModuleConfiguration.ACCOUNTING_CLOCK)
            Clock clock
    ) {
        return new AmplitudeTfjSignatureVerifier(
                properties,
                clock
        );
    }
}
