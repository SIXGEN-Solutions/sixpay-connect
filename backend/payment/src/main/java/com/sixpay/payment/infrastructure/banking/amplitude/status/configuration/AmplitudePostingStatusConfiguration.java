package com.sixpay.payment.infrastructure.banking.amplitude.status.configuration;

import com.sixpay.integration.http.HttpTimeoutPolicy;
import com.sixpay.integration.http.StandardRestClientFactory;
import com.sixpay.payment.application.port.output.banking.LookupGateway;
import com.sixpay.payment.application.reconciliation.PostingReconciliationService;
import com.sixpay.payment.application.reconciliation.PostingStatusQueryService;
import com.sixpay.payment.infrastructure.banking.amplitude.status.AmplitudePostingStatusClient;
import com.sixpay.payment.infrastructure.banking.amplitude.status.client.OAuth2PostingStatusAccessTokenProvider;
import com.sixpay.payment.infrastructure.banking.amplitude.status.client.PostingStatusAccessTokenProvider;
import com.sixpay.payment.infrastructure.banking.amplitude.status.client.RestAmplitudePostingStatusClient;
import com.sixpay.payment.infrastructure.banking.amplitude.status.mapper.AmplitudePostingStatusMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
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
        AmplitudePostingStatusProperties.class
)
@ConditionalOnProperty(
        prefix = AmplitudePostingStatusProperties.PREFIX,
        name = "enabled",
        havingValue = "true"
)
public class AmplitudePostingStatusConfiguration {

    @Bean
    PostingStatusAccessTokenProvider
    postingStatusAccessTokenProvider(
            OAuth2AuthorizedClientManager manager,
            AmplitudePostingStatusProperties properties
    ) {
        return new OAuth2PostingStatusAccessTokenProvider(
                manager,
                properties
        );
    }

    @Bean
    RestClient amplitudePostingStatusRestClient(
            StandardRestClientFactory factory,
            AmplitudePostingStatusProperties properties,
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
    AmplitudePostingStatusMapper amplitudePostingStatusMapper(
            ObjectMapper objectMapper
    ) {
        return new AmplitudePostingStatusMapper(
                objectMapper,
                Clock.systemUTC()
        );
    }

    @Bean
    AmplitudePostingStatusClient amplitudePostingStatusClient(
            RestClient amplitudePostingStatusRestClient,
            PostingStatusAccessTokenProvider tokenProvider,
            AmplitudePostingStatusProperties properties,
            AmplitudePostingStatusMapper mapper,
            ObjectMapper objectMapper
    ) {
        return new RestAmplitudePostingStatusClient(
                amplitudePostingStatusRestClient,
                tokenProvider,
                properties,
                mapper,
                objectMapper
        );
    }

    @Bean
    @ConditionalOnBean(LookupGateway.class)
    PostingStatusQueryService postingStatusQueryService(
            LookupGateway lookupGateway
    ) {
        return new PostingStatusQueryService(
                lookupGateway
        );
    }

    @Bean
    @ConditionalOnBean(PostingStatusQueryService.class)
    PostingReconciliationService postingReconciliationService(
            PostingStatusQueryService queryService
    ) {
        return new PostingReconciliationService(
                queryService,
                Clock.systemUTC()
        );
    }
}
