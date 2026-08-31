package com.sixpay.payment.infrastructure.banking.amplitude.confirmation.configuration;

import com.sixpay.integration.http.HttpTimeoutPolicy;
import com.sixpay.integration.http.StandardRestClientFactory;
import com.sixpay.payment.infrastructure.banking.amplitude.confirmation.AmplitudePaymentConfirmationClient;
import com.sixpay.payment.infrastructure.banking.amplitude.confirmation.client.*;
import com.sixpay.payment.infrastructure.banking.amplitude.confirmation.mapper.AmplitudePaymentConfirmationMapper;
import com.sixpay.payment.infrastructure.banking.amplitude.confirmation.validation.AmplitudePaymentConfirmationResponseValidator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AmplitudePaymentConfirmationProperties.class)
@ConditionalOnProperty(
        prefix = AmplitudePaymentConfirmationProperties.PREFIX,
        name = "enabled",
        havingValue = "true"
)
public class AmplitudePaymentConfirmationConfiguration {

    @Bean
    ConfirmationAccessTokenProvider confirmationAccessTokenProvider(
            OAuth2AuthorizedClientManager manager,
            AmplitudePaymentConfirmationProperties properties
    ) {
        return new OAuth2ConfirmationAccessTokenProvider(manager, properties);
    }

    @Bean
    RestClient amplitudePaymentConfirmationRestClient(
            StandardRestClientFactory factory,
            AmplitudePaymentConfirmationProperties properties,
            SslBundles sslBundles
    ) {
        return factory.create(
                properties.baseUrl(),
                new HttpTimeoutPolicy(properties.connectTimeout(), properties.readTimeout()),
                sslBundles.getBundle(properties.security().sslBundle()).createSslContext(),
                List.of()
        );
    }

    @Bean
    AmplitudePaymentConfirmationMapper amplitudePaymentConfirmationMapper() {
        return new AmplitudePaymentConfirmationMapper();
    }

    @Bean
    AmplitudePaymentConfirmationResponseValidator amplitudePaymentConfirmationResponseValidator() {
        return new AmplitudePaymentConfirmationResponseValidator();
    }

    @Bean
    AmplitudePaymentConfirmationClient amplitudePaymentConfirmationClient(
            RestClient amplitudePaymentConfirmationRestClient,
            ConfirmationAccessTokenProvider tokenProvider,
            AmplitudePaymentConfirmationProperties properties,
            AmplitudePaymentConfirmationMapper mapper,
            AmplitudePaymentConfirmationResponseValidator validator,
            ObjectMapper objectMapper
    ) {
        return new RestAmplitudePaymentConfirmationClient(
                amplitudePaymentConfirmationRestClient,
                tokenProvider,
                properties,
                mapper,
                validator,
                objectMapper
        );
    }
}
