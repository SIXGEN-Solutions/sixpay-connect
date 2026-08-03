package com.sixpay.customer.configuration;

import com.sixpay.customer.CustomerModule;
import com.sixpay.customer.verification.infrastructure.banking.configuration.BankingVerificationProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;

/**
 * Spring Boot auto-configuration entry point for the Customer module.
 */
@AutoConfiguration
@ComponentScan(basePackageClasses = CustomerModule.class)
@EnableConfigurationProperties(
        BankingVerificationProperties.class
)
public class CustomerModuleConfiguration {
}
