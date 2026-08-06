package com.sixpay.customer.configuration;

import com.sixpay.customer.CustomerModule;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 * Spring Boot auto-configuration entry point for the Customer module.
 *
 * <p>Optional banking infrastructure is activated by its own conditional
 * configuration and is not required by unrelated module tests.</p>
 */
@AutoConfiguration
@ComponentScan(basePackageClasses = CustomerModule.class)
public class CustomerModuleConfiguration {
}