package com.sixpay.customer.configuration;

import com.sixpay.customer.CustomerModule;
import com.sixpay.customer.verification.configuration.BankingVerificationProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;

@AutoConfiguration
@ComponentScan(basePackageClasses = CustomerModule.class)
@EnableConfigurationProperties(BankingVerificationProperties.class)
public class CustomerModuleConfiguration {
}
