package com.sixpay.payment.infrastructure.banking.amplitude.posting.configuration;

import com.sixpay.integration.http.HttpTimeoutPolicy;
import com.sixpay.integration.http.StandardRestClientFactory;
import com.sixpay.payment.infrastructure.banking.amplitude.posting.AmplitudePaymentEventAdapter;
import com.sixpay.payment.infrastructure.banking.amplitude.posting.AmplitudePaymentEventContextAdapter;
import com.sixpay.payment.infrastructure.banking.amplitude.posting.AmplitudePaymentEventRecoveryAdapter;
import com.sixpay.payment.infrastructure.banking.amplitude.posting.client.*;
import com.sixpay.payment.infrastructure.banking.amplitude.posting.mapper.AmplitudePaymentEventMapper;
import com.sixpay.payment.application.port.output.banking.PaymentEventContextPort;
import com.sixpay.payment.application.port.output.banking.PaymentEventExecutionPort;
import com.sixpay.payment.application.port.output.banking.PaymentEventRecoveryPort;
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
@EnableConfigurationProperties(
        AmplitudePostingProperties.class
)
@ConditionalOnProperty(
        prefix = AmplitudePostingProperties.PREFIX,
        name = "enabled",
        havingValue = "true"
)
public class AmplitudePostingConfiguration {

    @Bean
    PostingAccessTokenProvider postingAccessTokenProvider(
            OAuth2AuthorizedClientManager manager,
            AmplitudePostingProperties properties
    ) {
        return new OAuth2PostingAccessTokenProvider(
                manager,
                properties
        );
    }

    @Bean
    RestClient amplitudePostingRestClient(
            StandardRestClientFactory factory,
            AmplitudePostingProperties properties,
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
    AmplitudePaymentEventRecoveryClient amplitudePaymentEventRecoveryClient(
            RestClient amplitudePostingRestClient,
            PostingAccessTokenProvider tokenProvider,
            AmplitudePostingProperties properties
    ) {
        return new RestAmplitudePaymentEventRecoveryClient(
                amplitudePostingRestClient,
                tokenProvider,
                properties
        );
    }

    @Bean
    PaymentEventRecoveryPort paymentEventRecoveryPort(
            AmplitudePaymentEventRecoveryClient client
    ) {
        return new AmplitudePaymentEventRecoveryAdapter(client);
    }

    @Bean
    AmplitudePaymentEventContextClient amplitudePaymentEventContextClient(
            RestClient amplitudePostingRestClient,
            PostingAccessTokenProvider tokenProvider
    ) {
        return new RestAmplitudePaymentEventContextClient(
                amplitudePostingRestClient,
                tokenProvider
        );
    }

    @Bean
    PaymentEventContextPort paymentEventContextPort(
            AmplitudePaymentEventContextClient client
    ) {
        return new AmplitudePaymentEventContextAdapter(client);
    }

    @Bean
    AmplitudePaymentEventMapper amplitudePaymentEventMapper() {
        return new AmplitudePaymentEventMapper();
    }

    @Bean
    AmplitudePaymentEventClient amplitudePaymentEventClient(
            RestClient amplitudePostingRestClient,
            PostingAccessTokenProvider tokenProvider,
            AmplitudePostingProperties properties
    ) {
        return new RestAmplitudePaymentEventClient(
                amplitudePostingRestClient,
                tokenProvider,
                properties
        );
    }

    @Bean
    PaymentEventExecutionPort paymentEventExecutionPort(
            AmplitudePaymentEventClient client,
            AmplitudePaymentEventMapper mapper
    ) {
        return new AmplitudePaymentEventAdapter(client, mapper);
    }

}
