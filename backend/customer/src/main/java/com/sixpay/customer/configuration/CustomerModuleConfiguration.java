package com.sixpay.customer.configuration;

import com.sixpay.customer.CustomerModule;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 * Spring Boot auto-configuration entry point for the Customer module.
 *
 * <p>The configuration activates the Customer Verification and Observed
 * Customer capabilities when the module is present on the application
 * classpath.</p>
 */
@AutoConfiguration
@ComponentScan(basePackageClasses = CustomerModule.class)
public class CustomerModuleConfiguration {
}
