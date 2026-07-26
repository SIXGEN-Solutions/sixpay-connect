package com.sixpay.integration.configuration;

import com.sixpay.integration.http.DefaultRestClientFactory;
import com.sixpay.integration.http.RestClientFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition
        .ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition
        .ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition
        .ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.web.client
        .RestClientAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClient;

/**
 * Auto-configuration for SIXPAY external integrations.
 */
@AutoConfiguration(after = RestClientAutoConfiguration.class)
@ConditionalOnClass(RestClient.class)
public class SixpayIntegrationAutoConfiguration {

    @Bean
    @ConditionalOnBean(RestClient.Builder.class)
    @ConditionalOnMissingBean(RestClientFactory.class)
    RestClientFactory restClientFactory(
            RestClient.Builder restClientBuilder
    ) {
        return new DefaultRestClientFactory(
                restClientBuilder
        );
    }
}