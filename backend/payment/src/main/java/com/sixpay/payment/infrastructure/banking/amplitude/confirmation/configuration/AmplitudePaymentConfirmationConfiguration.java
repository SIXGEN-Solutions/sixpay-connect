package com.sixpay.payment.infrastructure.banking.amplitude.confirmation.configuration;

import com.sixpay.integration.http.HttpTimeoutPolicy;
import com.sixpay.integration.http.StandardRestClientFactory;
import com.sixpay.payment.application.port.output.banking.PaymentConfirmationGateway;
import com.sixpay.payment.infrastructure.banking.amplitude.confirmation.AmplitudePaymentConfirmationClient;
import com.sixpay.payment.infrastructure.banking.amplitude.confirmation.DedicatedAmplitudePaymentConfirmationAdapter;
import com.sixpay.payment.infrastructure.banking.amplitude.confirmation.client.*;
import com.sixpay.payment.infrastructure.banking.amplitude.confirmation.mapper.AmplitudePaymentConfirmationMapper;
import com.sixpay.payment.infrastructure.banking.amplitude.confirmation.validation.AmplitudePaymentConfirmationResponseValidator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.beans.factory.ObjectProvider;
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
            ObjectProvider<OAuth2AuthorizedClientManager> managerProvider,
            AmplitudePaymentConfirmationProperties properties
    ) {
        if (!properties.security().oauth2Enabled()) return () -> null;
        OAuth2AuthorizedClientManager manager = managerProvider.getIfAvailable();
        if (manager == null) throw new IllegalStateException("OAuth2 manager required when confirmation OAuth2 is enabled");
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
                properties.security().mtlsEnabled()
                        ? sslBundles.getBundle(properties.security().sslBundle()).createSslContext() : null,
                List.of(),
                properties.security().allowInsecureHttp()
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

    @Bean
    @ConditionalOnMissingBean(PaymentConfirmationGateway.class)
    PaymentConfirmationGateway paymentConfirmationGateway(
            AmplitudePaymentConfirmationClient client
    ) {
        return new DedicatedAmplitudePaymentConfirmationAdapter(client);
    }
}
