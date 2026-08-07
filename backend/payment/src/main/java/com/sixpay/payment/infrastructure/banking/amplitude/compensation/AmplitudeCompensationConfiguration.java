package com.sixpay.payment.infrastructure.banking.amplitude.compensation;

import com.sixpay.integration.http.HttpTimeoutPolicy;
import com.sixpay.integration.http.StandardRestClientFactory;
import com.sixpay.payment.infrastructure.banking.amplitude.release.*;
import com.sixpay.payment.infrastructure.banking.amplitude.release.client.RestAmplitudeFundsReleaseClient;
import com.sixpay.payment.infrastructure.banking.amplitude.release.mapper.AmplitudeFundsReleaseMapper;
import com.sixpay.payment.infrastructure.banking.amplitude.reversal.*;
import com.sixpay.payment.infrastructure.banking.amplitude.reversal.client.RestAmplitudeReversalClient;
import com.sixpay.payment.infrastructure.banking.amplitude.reversal.mapper.AmplitudeReversalMapper;
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
        AmplitudeCompensationProperties.class
)
@ConditionalOnProperty(
        prefix = AmplitudeCompensationProperties.PREFIX,
        name = "enabled",
        havingValue = "true"
)
public class AmplitudeCompensationConfiguration {

    @Bean
    CompensationAccessTokenProvider
    compensationAccessTokenProvider(
            OAuth2AuthorizedClientManager manager,
            AmplitudeCompensationProperties properties
    ) {
        return new OAuth2CompensationAccessTokenProvider(
                manager,
                properties.security().oauth2RegistrationId()
        );
    }

    @Bean
    RestClient amplitudeCompensationRestClient(
            StandardRestClientFactory factory,
            AmplitudeCompensationProperties properties,
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
    AmplitudeFundsReleaseMapper amplitudeFundsReleaseMapper(
            ObjectMapper objectMapper
    ) {
        return new AmplitudeFundsReleaseMapper(
                objectMapper,
                Clock.systemUTC()
        );
    }

    @Bean
    AmplitudeReversalMapper amplitudeReversalMapper(
            ObjectMapper objectMapper
    ) {
        return new AmplitudeReversalMapper(
                objectMapper,
                Clock.systemUTC()
        );
    }

    @Bean
    AmplitudeFundsReleaseClient amplitudeFundsReleaseClient(
            RestClient amplitudeCompensationRestClient,
            CompensationAccessTokenProvider tokenProvider,
            AmplitudeCompensationProperties properties,
            AmplitudeFundsReleaseMapper mapper,
            ObjectMapper objectMapper
    ) {
        return new RestAmplitudeFundsReleaseClient(
                amplitudeCompensationRestClient,
                tokenProvider,
                properties,
                mapper,
                objectMapper
        );
    }

    @Bean
    AmplitudeReversalClient amplitudeReversalClient(
            RestClient amplitudeCompensationRestClient,
            CompensationAccessTokenProvider tokenProvider,
            AmplitudeCompensationProperties properties,
            AmplitudeReversalMapper mapper,
            ObjectMapper objectMapper
    ) {
        return new RestAmplitudeReversalClient(
                amplitudeCompensationRestClient,
                tokenProvider,
                properties,
                mapper,
                objectMapper
        );
    }
}
