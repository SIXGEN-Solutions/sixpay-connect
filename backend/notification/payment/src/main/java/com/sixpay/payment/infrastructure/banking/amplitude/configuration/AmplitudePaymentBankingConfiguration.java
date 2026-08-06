package com.sixpay.payment.infrastructure.banking.amplitude.configuration;

import com.sixpay.integration.http.HttpTimeoutPolicy;
import com.sixpay.integration.http.StandardRestClientFactory;
import com.sixpay.integration.messaging.json.IntegrationJsonSerializer;
import com.sixpay.integration.resilience.RetryingIntegrationExecutor;
import com.sixpay.payment.infrastructure.banking.amplitude.AmplitudeAccountFundsClient;
import com.sixpay.payment.infrastructure.banking.amplitude.client.*;
import com.sixpay.payment.infrastructure.banking.amplitude.mapper.AmplitudeAccountFundsMapper;
import com.sixpay.payment.infrastructure.banking.amplitude.validation.AmplitudeAccountFundsResponseValidator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.util.List;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(
        AmplitudePaymentBankingProperties.class
)
@ConditionalOnProperty(
        prefix = AmplitudePaymentBankingProperties.PREFIX,
        name = "enabled",
        havingValue = "true"
)
public class AmplitudePaymentBankingConfiguration {

    @Bean
    PaymentCoreBankingAccessTokenProvider
    paymentCoreBankingAccessTokenProvider(
            OAuth2AuthorizedClientManager manager,
            AmplitudePaymentBankingProperties properties
    ) {
        return new OAuth2PaymentCoreBankingAccessTokenProvider(
                manager,
                properties
        );
    }

    @Bean
    RestClient amplitudePaymentBankingRestClient(
            StandardRestClientFactory factory,
            AmplitudePaymentBankingProperties properties,
            SslBundles sslBundles
    ) {
        return factory.create(
                properties.baseUrl(),
                new HttpTimeoutPolicy(
                        properties.connectTimeout(),
                        properties.readTimeout()
                ),
                sslBundles.getBundle(
                        properties.security().sslBundle()
                ).createSslContext(),
                List.of()
        );
    }

    @Bean
    AmplitudeAccountFundsMapper amplitudeAccountFundsMapper(
            ObjectMapper objectMapper
    ) {
        return new AmplitudeAccountFundsMapper(
                objectMapper,
                Clock.systemUTC()
        );
    }

    @Bean
    AmplitudeAccountFundsResponseValidator
    amplitudeAccountFundsResponseValidator(
            AmplitudePaymentBankingProperties properties
    ) {
        return new AmplitudeAccountFundsResponseValidator(
                properties
        );
    }

    @Bean
    AmplitudeAccountFundsClient amplitudeAccountFundsClient(
            RestClient amplitudePaymentBankingRestClient,
            PaymentCoreBankingAccessTokenProvider tokenProvider,
            AmplitudePaymentBankingProperties properties,
            IntegrationJsonSerializer serializer,
            AmplitudeAccountFundsResponseValidator validator,
            AmplitudeAccountFundsMapper mapper,
            RetryingIntegrationExecutor retryingExecutor
    ) {
        return new RestAmplitudeAccountFundsClient(
                amplitudePaymentBankingRestClient,
                tokenProvider,
                properties,
                serializer,
                validator,
                mapper,
                retryingExecutor
        );
    }
}
