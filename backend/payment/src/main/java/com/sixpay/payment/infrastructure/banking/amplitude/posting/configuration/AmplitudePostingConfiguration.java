package com.sixpay.payment.infrastructure.banking.amplitude.posting.configuration;

import com.sixpay.integration.http.HttpTimeoutPolicy;
import com.sixpay.integration.http.StandardRestClientFactory;
import com.sixpay.payment.infrastructure.banking.amplitude.posting.AmplitudePostingClient;
import com.sixpay.payment.infrastructure.banking.amplitude.posting.client.*;
import com.sixpay.payment.infrastructure.banking.amplitude.posting.mapper.AmplitudePostingMapper;
import com.sixpay.payment.infrastructure.banking.amplitude.posting.validation.AmplitudePostingResponseValidator;
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
    AmplitudePostingMapper amplitudePostingMapper(
            ObjectMapper objectMapper
    ) {
        return new AmplitudePostingMapper(
                objectMapper,
                Clock.systemUTC()
        );
    }

    @Bean
    AmplitudePostingResponseValidator
    amplitudePostingResponseValidator(
            AmplitudePostingProperties properties
    ) {
        return new AmplitudePostingResponseValidator(
                properties
        );
    }

    @Bean
    AmplitudePostingClient amplitudePostingClient(
            RestClient amplitudePostingRestClient,
            PostingAccessTokenProvider tokenProvider,
            AmplitudePostingProperties properties,
            AmplitudePostingMapper mapper,
            AmplitudePostingResponseValidator validator,
            ObjectMapper objectMapper
    ) {
        return new RestAmplitudePostingClient(
                amplitudePostingRestClient,
                tokenProvider,
                properties,
                mapper,
                validator,
                objectMapper
        );
    }
}
